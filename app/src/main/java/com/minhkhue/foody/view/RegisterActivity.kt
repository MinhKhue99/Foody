package com.minhkhue.foody.view

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import com.firebase.ui.auth.AuthUI
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.database.*
import com.minhkhue.foody.R
import com.minhkhue.foody.model.User
import com.minhkhue.foody.utils.Constants
import dmax.dialog.SpotsDialog
import io.reactivex.disposables.CompositeDisposable

class RegisterActivity : AppCompatActivity() {
    private lateinit var firebaseAuth: FirebaseAuth
    private lateinit var listener: FirebaseAuth.AuthStateListener
    private lateinit var dialog: AlertDialog
    private val compositeDisposable = CompositeDisposable()
    private lateinit var userRef: DatabaseReference
    private var provider: List<AuthUI.IdpConfig>? = null

    override fun onStart() {
        super.onStart()
        firebaseAuth.addAuthStateListener(listener)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        init()
    }

    override fun onStop() {
        firebaseAuth.removeAuthStateListener(listener)
        compositeDisposable.clear()
        super.onStop()
    }

    private fun init() {
        provider =
            listOf(AuthUI.IdpConfig.PhoneBuilder().build(), AuthUI.IdpConfig.EmailBuilder().build())
        userRef = FirebaseDatabase.getInstance().getReference(Constants.USER_REFERENCE)
        firebaseAuth = FirebaseAuth.getInstance()
        dialog = SpotsDialog.Builder().setContext(this).setCancelable(false).build()
        listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
            val user = firebaseAuth.currentUser
            if (user != null) {
                checkUserFromFirebase(user)
            } else {
                phoneLogin()
            }
        }
    }

    private fun phoneLogin() {
        registerResult.launch(
            AuthUI.getInstance().createSignInIntentBuilder()
                .setTheme(R.style.loginTheme)
                .setLogo(R.drawable.logo)
                .setAvailableProviders(provider!!)
                .build()
        )
    }

    private var registerResult =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                FirebaseAuth.getInstance().currentUser
            } else {
                Toast.makeText(this@RegisterActivity, "Failed to sign in", Toast.LENGTH_SHORT)
                    .show()
            }
        }

    private fun checkUserFromFirebase(user: FirebaseUser) {
        dialog.show()
        userRef.child(user.uid)
            .addListenerForSingleValueEvent(object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    if (snapshot.exists()) {
                        val userModel = snapshot.getValue(User::class.java)
                        goToMainActivity(userModel)
                    } else {
                        showRegisterDialog(user)
                    }

                }

                override fun onCancelled(error: DatabaseError) {
                    Toast.makeText(this@RegisterActivity, "" + error.message, Toast.LENGTH_SHORT)
                        .show()
                }

            })
    }

    private fun showRegisterDialog(user: FirebaseUser) {
        val builder = androidx.appcompat.app.AlertDialog.Builder(this)
        builder.setTitle("REGISTER")
        builder.setMessage("Please fill in information")
        val itemView = LayoutInflater.from(this@RegisterActivity)
            .inflate(R.layout.layout_register, null)
        val edtName = itemView.findViewById<EditText>(R.id.edtName)
        val edtPhone = itemView.findViewById<EditText>(R.id.edtPhone)
        val edtAddress = itemView.findViewById<EditText>(R.id.edtAddress)

        edtPhone.setText(user.phoneNumber.toString())
        builder.setView(itemView)
        builder.setNegativeButton("Cancel") { dialogInterface, _ -> dialogInterface.dismiss() }
        builder.setPositiveButton("Register") { dialogInterface, _ ->
            if (TextUtils.isDigitsOnly(edtName.text.toString())) {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            } else if (TextUtils.isDigitsOnly(edtAddress.text.toString())) {
                Toast.makeText(this, "Please enter your address", Toast.LENGTH_SHORT).show()
                return@setPositiveButton
            }
            val userModel = User()
            userModel.uid = user.uid
            userModel.name = edtName.text.toString()
            userModel.phone = edtPhone.text.toString()
            userModel.address = edtAddress.text.toString()

            userRef.child(user.uid)
                .setValue(userModel)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        dialogInterface.dismiss()
                        Toast.makeText(this, "Congratulation! Register Success", Toast.LENGTH_SHORT)
                            .show()
                        goToMainActivity(userModel)
                    }
                }
        }
        val dialog = builder.create()
        dialog.show()
    }

    private fun goToMainActivity(userModel: User?) {
        Constants.currentUser = userModel
        startActivity(Intent(this, MainActivity::class.java))
        finish()
    }
}

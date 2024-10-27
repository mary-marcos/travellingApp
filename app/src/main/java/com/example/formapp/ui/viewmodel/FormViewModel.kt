package com.example.formapp.ui.viewmodel

// FormViewModel.kt
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.formapp.model.User
import com.example.formapp.model.UserFormState
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.File

class FormViewModel(private val firestore: FirebaseFirestore) : ViewModel() {
   // private val storage: FirebaseStorage

    private val _user = MutableStateFlow(User())
    val user: StateFlow<User> get() = _user

    private val _formState = MutableStateFlow<UserFormState>(UserFormState.Invalid)
    val formState: StateFlow<UserFormState> get() = _formState

   // fun setUserDetails(name: String, age: String, email: String, phone: String, whatsapp: String, cv: String, passportImage: String, voiceRecord: String, jobTitle: String, startYear: String, jobAddress: String)
   fun setUserDetails(user:User)
    {
        _user.value =user
    }

    fun validateForm() {
        viewModelScope.launch {
            val isValid = withContext(Dispatchers.Default) {
                validateName() && validateAge() && validateEmail() && validatePhone() && validateWhatsApp()
            }
            _formState.value = if (isValid) UserFormState.Valid else UserFormState.Invalid
        }
    }

    private fun validateName() = _user.value.name.isNotBlank()

    private fun validateAge() = _user.value.age > 16

    private fun validateEmail() =
        _user.value.email.isNotBlank() && android.util.Patterns.EMAIL_ADDRESS.matcher(_user.value.email).matches()

    private fun validatePhone(): Boolean {
        val phone = _user.value.phone
        return phone.isNotBlank() &&
                phone.length == 11 &&
                phone.matches(Regex("^01[0-9]{9}\$")) // Matches Egyptian mobile numbers
    }

    private fun validateWhatsApp(): Boolean {
        val whatsapp = _user.value.whatsapp
        return whatsapp.isNotBlank() &&
                whatsapp.length == 11 &&
                whatsapp.matches(Regex("^01[0-9]{9}\$")) // Matches Egyptian WhatsApp numbers
    }

    fun storeUserData() {
        val user = _user.value
        Log.d("storeUserData", "Storing User: $user")
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) {
                    firestore.collection("users").add(user).await()
                }
                _formState.value = UserFormState.DataStored // Notify success
                Log.d("storeUserData", "User stored successfully: $user")
            } catch (e: Exception) {
                Log.e("storeUserData", "Error storing user data", e)
                _formState.value = UserFormState.DataStoreError // Notify failure
            }
        }
    }

//    private suspend fun uploadImage(filePath: String): String? {
//        return try {
//            val fileUri = Uri.fromFile(File(filePath))
//            val storageRef = storage.reference.child("images/${fileUri.lastPathSegment}")
//            val uploadTask = storageRef.putFile(fileUri).await()
//            storageRef.downloadUrl.await().toString()
//        } catch (e: Exception) {
//            Log.e("FormViewModel", "Error uploading image", e)
//            null
//        }
//    }

}


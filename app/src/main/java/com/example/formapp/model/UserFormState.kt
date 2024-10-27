package com.example.formapp.model

sealed class UserFormState {
    object Valid : UserFormState()
    object Invalid : UserFormState()
    object DataStored : UserFormState()
    object DataStoreError : UserFormState()

}
package com.example.formapp.model

data class User(
    val name: String = "",
    val age: Int = 0,
    val email: String = "",
    val phone: String = "",
    val whatsapp: String = "",
    val nationalID:String="",
    val cv: String = "", // URL or path to the CV file
    val passportImage: String = "", // URL or path to the passport image
    val voiceRecord: String = "", // URL or path to the voice record
    val jobTitle: String = "", // Job title
    val startYear: String = "", // Start year of the job
    val jobAddress: String = "", // Job address
    val isCompleteStudy:Boolean=false,
    val isWork:Boolean=false,
    val proofStudyImage: String = "",
    val secondarySchoolImage: String = "",
    val graduationCertificateImg: String = "",
    val transcriptOfGradesImg: String = "",
)
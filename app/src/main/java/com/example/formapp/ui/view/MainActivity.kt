package com.example.formapp.ui.view

import android.Manifest
import android.app.Activity
import android.app.DatePickerDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaRecorder
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast

import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.formapp.R
import com.example.formapp.databinding.ActivityMainBinding
import com.example.formapp.model.User
import com.example.formapp.model.UserFormState
import com.example.formapp.ui.viewmodel.FormViewModel
import com.example.formapp.ui.viewmodel.ViewModelFactory
import com.google.firebase.FirebaseApp
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import java.io.IOException
import java.util.Calendar

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private val viewModel: FormViewModel by viewModels { ViewModelFactory(FirebaseFirestore.getInstance()) }

    private var cvUri: Uri? = null
    private var passportUri: Uri? = null
    private var audioFilePath: String? = null
    private var mediaRecorder: MediaRecorder? = null

    //workJop
    private  var jopstartYear:EditText?=null
    var joptittle :EditText?=null
    var jopAdress :EditText?=null
    var cvDescription:EditText?=null
    var isWork:Boolean?=null

    //study
    private  var  isCompleteStudy:Boolean?=null
    private  var proofOfStudyimg: ImageView?= null
    private  var generalSecondarySchoolImg: ImageView?=null
    private var graduationCertify: ImageView?=null
    private var  transcriptGrades: ImageView?=null

    private var proofOfStudyUri: Uri? = null
    private var generalSecondarySchoolUri: Uri? = null
    private var graduationCertifyUri: Uri? = null
    private var transcriptGradesUri: Uri? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        FirebaseApp.initializeApp(this)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Request audio and storage permissions
        requestPermissions()

        setupListeners()
        observeFormState()
    }

    private fun requestPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECORD_AUDIO)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.WRITE_EXTERNAL_STORAGE)
        }

        if (permissions.isNotEmpty()) {
            ActivityCompat.requestPermissions(this, permissions.toTypedArray(), REQUEST_RECORD_AUDIO_PERMISSION)
        }
    }

    private fun setupListeners() {
        //showWorkdialog
        binding.radioWorking.setOnClickListener{showAddjopDetailsDialog()
        isWork=true}
        binding.radioNotWorking.setOnClickListener { isWork=false }
        binding.radioCompleteStudy.setOnClickListener {
            isCompleteStudy=true
            showAddCompleteStudyDetailsDialog() }
        binding.radioStudy.setOnClickListener { isCompleteStudy=false }


        // CV Upload Button
        binding.btnUploadCV.setOnClickListener { pickFile(CV_PICKER_REQUEST) }

        // Passport Image Upload Button
        binding.btnUploadPassport.setOnClickListener { pickFile(PASSPORT_PICKER_REQUEST) }

        // Record Voice Button
        binding.btnRecordVoice.setOnTouchListener { _, event ->
            when (event.action) {
                MotionEvent.ACTION_DOWN -> startRecording()
                MotionEvent.ACTION_UP -> stopRecording()
            }
            true
        }

        // Submit Button
        binding.btnSubmit.setOnClickListener {
            Log.d("MainActivity", "Submit button clicked")
           collectUserInput()
          viewModel.validateForm()

          //  setDataToFireBase()
        }
    }

//    private fun setDataToFireBase() {
//      if (isValidData()){
//          observeResultFirebase()
//          viewModel.setUser()
//      }else{}
//    }

    private fun pickFile(requestCode: Int) {
        val intent = Intent(Intent.ACTION_GET_CONTENT).apply {
            type = if (requestCode == CV_PICKER_REQUEST) "application/pdf" else "image/*"
        }
        startActivityForResult(intent, requestCode)
    }

    private fun startRecording() {
        // Release any existing MediaRecorder before starting a new recording
        mediaRecorder?.release()
        mediaRecorder = null

        try {
            audioFilePath = "${externalCacheDir?.absolutePath}/voice_record.3gp"
            mediaRecorder = MediaRecorder().apply {
                setAudioSource(MediaRecorder.AudioSource.MIC)
                setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP)
                setAudioEncoder(MediaRecorder.AudioEncoder.AMR_NB)
                setOutputFile(audioFilePath)
                prepare()
                start()
            }
            Toast.makeText(this, "Recording started", Toast.LENGTH_SHORT).show()
        } catch (e: IOException) {
            Toast.makeText(this, "Failed to start recording: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("MainActivity", "IOException during recording", e)
        } catch (e: IllegalStateException) {
            Toast.makeText(this, "Recording error: ${e.message}", Toast.LENGTH_SHORT).show()
            Log.e("MainActivity", "IllegalStateException during recording", e)
        }
    }

    private fun stopRecording() {
        mediaRecorder?.apply {
            try {
                stop()
                Toast.makeText(this@MainActivity, "Recording stopped", Toast.LENGTH_SHORT).show()
            } catch (e: RuntimeException) {
                Toast.makeText(this@MainActivity, "Failed to stop recording: ${e.message}", Toast.LENGTH_SHORT).show()
                Log.e("MainActivity", "RuntimeException during stopping", e)
            } finally {
                release()
                mediaRecorder = null // Set to null after releasing
            }
        } ?: Toast.makeText(this, "No recording in progress", Toast.LENGTH_SHORT).show()
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == RESULT_OK && data != null) {
            when (requestCode) {
                CV_PICKER_REQUEST -> {
                    cvUri = data.data
                    binding.etCV.setText(cvUri.toString())
                }

                PASSPORT_PICKER_REQUEST -> {
                    passportUri = data.data
                    binding.etPassportImage.setText(passportUri.toString())
                }




                proof_study_PICKER_REQUEST -> {
                    proofOfStudyUri =  data.data
                    proofOfStudyimg?.setImageURI(proofOfStudyUri)
                }
                SECONDARY_SCHOOL_GRADES_PICKER_REQUEST -> {
                    generalSecondarySchoolUri =  data.data
                    generalSecondarySchoolImg?.setImageURI(generalSecondarySchoolUri)
                }
                GRADUATION_CERTIFICATE_PICKER_REQUEST -> {
                    graduationCertifyUri = data.data
                    graduationCertify?.setImageURI(graduationCertifyUri)
                }
                TRANSCRIPT_GRADES_PICKER_REQUEST -> {
                    transcriptGradesUri =  data.data
                    transcriptGrades?.setImageURI(transcriptGradesUri)}




            }
        }
    }

    //     User(
    //            name,
    //            age.toIntOrNull() ?: 0,
    //            email,
    //            phone,
    //            whatsapp,
    //            cv,
    //            passportImage,
    //            voiceRecord,
    //            jobTitle,
    //            startYear.toIntOrNull() ?: 0,
    //            jobAddress
    //        )




    //
    //                name = binding.etName.text.toString(),
    //                age = binding.etAge.text.toString(),
    //                email = binding.etEmail.text.toString(),
    //                phone = binding.etPhone.text.toString(),
    //                whatsapp = binding.etWhatsApp.text.toString(),
    //                cv = cvUri.toString(),
    //                passportImage = passportUri.toString(),
    //                voiceRecord = audioFilePath ?: "",
    //                jobTitle = binding.etJobTitle.text.toString(),
    //                startYear = binding.etStartYear.text.toString(),
    //                jobAddress = binding.etJobAddress.text.toString(),

    private fun collectUserInput() {
        if(isValidData()){
            viewModel.setUserDetails(User(
                name = binding.etName.text.toString(),
                age = binding.etAge.text.toString().toIntOrNull() ?: 0,
                email = binding.etEmail.text.toString(),
                phone = binding.etPhone.text.toString(),
                whatsapp = binding.etWhatsApp.text.toString(),
               // cv = cvUri.toString(),
                passportImage = passportUri.toString(),
                voiceRecord = audioFilePath ?: "",
                isWork = isWork?:false,
                jobTitle = joptittle?.text.toString(),
                startYear = jopstartYear?.text.toString(),
                jobAddress = jopAdress?.text.toString(),
                cv=cvDescription?.text.toString(),
                graduationCertificateImg = graduationCertifyUri.toString(),
                transcriptOfGradesImg =transcriptGradesUri.toString() ,
                secondarySchoolImage = generalSecondarySchoolUri.toString(),
                proofStudyImage = proofOfStudyUri.toString(),
                isCompleteStudy = isCompleteStudy?:false,
                nationalID = binding.etNationalId.text.toString(),



                )  )
        }

    }

    private fun observeFormState() {
        lifecycleScope.launch {
            viewModel.formState.collect { state ->
                when (state) {
                    is UserFormState.Valid -> {
                        Log.d("MainActivity", "Form is valid, storing data...")
                        viewModel.storeUserData() // Store data if form is valid
                    }
                    is UserFormState.Invalid -> showErrorToast("Please fill all fields correctly!")
                    is UserFormState.DataStored -> {
                        Toast.makeText(this@MainActivity, "Data Saved!", Toast.LENGTH_SHORT).show()
                        clearInputFields() // Clear fields after successful save
                    }
                    is UserFormState.DataStoreError -> showErrorToast("Error saving data!")
                }
            }
        }
    }

    private fun clearInputFields() {
        binding.etName.text?.clear()
        binding.etAge.text?.clear()
        binding.etEmail.text?.clear()
        binding.etPhone.text?.clear()
        binding.etWhatsApp.text?.clear()
        binding.etCV.text?.clear()
        binding.etPassportImage.text?.clear()
        binding.etNationalId.text?.clear()
      joptittle?.text?.clear()
        jopstartYear?.text?.clear()
        jopAdress?.text?.clear()
    }

    private fun showErrorToast(message: String) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }



    fun isValidData(): Boolean {
        var isValid = true

     if(isCompleteStudy==null){
       isValid = false
         binding.radioStudy.error="Study Status is Required"
         Toast.makeText(this,"Studying Status is Required",Toast.LENGTH_SHORT).show()
           }else{  binding.radioStudy.error=null}

        if(isWork==null){
            isValid = false
            binding.radioWorking.error="work Status is Required"
            Toast.makeText(this,"work Status is Required",Toast.LENGTH_SHORT).show()
        }else{binding.radioWorking.error=null}

          if( binding.etNationalId.text.isNullOrBlank()||binding.etNationalId.text.length!=14){

              binding.etNationalId.error = "National ID must be exactly 14 digits"
              isValid = false
          } else {
              binding.etNationalId.error = null
          }
        if (binding.etName.text.isNullOrBlank()) {
            binding.etName.error = "Title is required"
            isValid = false
        } else {
            binding.etName.error = null
        }



        if (binding.etAge.text.isNullOrBlank()) {
            binding.etAge.error = "Description is required"
            isValid = false
        } else {
            binding.etAge.error = null
        }


        if (binding.etPhone.text.isNullOrBlank()) {
            binding.etPhone.error = "Description is required"
            isValid = false
        } else {
            binding.etPhone.error = null
        }


        if (binding.etWhatsApp.text.isNullOrBlank()) {
            binding.etWhatsApp.error = "Description is required"
            isValid = false
        } else {
            binding.etWhatsApp.error = null
        }


        if (binding.etEmail.text.isNullOrBlank()) {
            binding.etEmail.error = "Description is required"
            isValid = false
        } else {
            binding.etEmail.error = null
        }

        return isValid
    }


    private fun showAddjopDetailsDialog() {

      var  dialog = Dialog(this@MainActivity).apply {

            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)
            setContentView(R.layout.jop_dialog)


            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT // Wrap height
            )

            var cancelBtn = findViewById<Button>(R.id.confirm_jop_button )
            cancelBtn.setOnClickListener {
                dismiss()
            }
           joptittle = findViewById(R.id.etJobTitle)
           jopAdress = findViewById<EditText>(R.id.etJobAddress)
            jopstartYear = findViewById<EditText>(R.id.etStartYear)
           cvDescription = findViewById<EditText>(R.id.cvdescription_et)

          jopstartYear?.setOnClickListener { showDatePicker(it) }

          show()
        }
    }

    private fun showDatePicker(view: View) {
        val c: Calendar = Calendar.getInstance()
        val mYear = c.get(Calendar.YEAR)
        val mMonth = c.get(Calendar.MONTH)
        val mDay = c.get(Calendar.DAY_OF_MONTH)


        val datePickerDialog = DatePickerDialog(
            this@MainActivity,
            { _, year, monthOfYear, dayOfMonth ->

                val strDate = year.toString() + "-" + (monthOfYear + 1) + "-" + dayOfMonth.toString()
                if (view.id == jopstartYear?.id) {
                    jopstartYear?.setText(strDate)
                }
//                else {
//                    binding.endsAtEditText.setText(strDate)
//                }
            },
            mYear!!,
            mMonth!!,
            mDay!!
        )
        datePickerDialog.datePicker.maxDate = System.currentTimeMillis()
      //  datePickerDialog.datePicker.minDate = System.currentTimeMillis() - 1000;
        datePickerDialog.show()
    }


    private fun showAddCompleteStudyDetailsDialog() {

        var  dialog = Dialog(this@MainActivity).apply {

            requestWindowFeature(Window.FEATURE_NO_TITLE)
            setCancelable(false)
            setContentView(R.layout.complete_study_dialog)


            window?.setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT // Wrap height
            )

            var cancelBtn = findViewById<Button>(R.id.confirm_button_study )
            cancelBtn.setOnClickListener {
                dismiss()
            }
            proofOfStudyimg = findViewById(R.id.proof_english_img)
            generalSecondarySchoolImg = findViewById(R.id.Secondary_school_img)
            graduationCertify = findViewById(R.id.graduation_certificate_img)
            transcriptGrades = findViewById(R.id.transcript_grade_img)

            proofOfStudyimg?.let { it.setOnClickListener { pickFile(proof_study_PICKER_REQUEST) } }
            generalSecondarySchoolImg?.let { it.setOnClickListener { pickFile(SECONDARY_SCHOOL_GRADES_PICKER_REQUEST) } }
            graduationCertify?.let { it.setOnClickListener { pickFile(GRADUATION_CERTIFICATE_PICKER_REQUEST) } }
            transcriptGrades?.let { it.setOnClickListener { pickFile(TRANSCRIPT_GRADES_PICKER_REQUEST) } }

            show()
        }
    }



    companion object {
        const val CV_PICKER_REQUEST = 101
        const val PASSPORT_PICKER_REQUEST = 102
        const val proof_study_PICKER_REQUEST = 104
        const val SECONDARY_SCHOOL_GRADES_PICKER_REQUEST = 105
        const val GRADUATION_CERTIFICATE_PICKER_REQUEST = 106
        const val TRANSCRIPT_GRADES_PICKER_REQUEST = 107
        const val REQUEST_RECORD_AUDIO_PERMISSION = 200
    }
}

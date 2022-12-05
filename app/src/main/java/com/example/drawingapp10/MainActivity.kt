    package com.example.drawingapp10

import android.Manifest
import android.app.AlertDialog
import android.app.Dialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.Debug
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.*
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream

    class MainActivity : AppCompatActivity() {

    private  var drawingView : DrawingView? = null
private lateinit var  btn_ChoseBrushsize: ImageButton
        private lateinit var  btn_ChoseBrushcolor: ImageButton
        private var mImageButtonCurrentPaint: ImageButton? = null
        private  var brushColorDialog: Dialog? = null
        private lateinit var  btn_eraser: ImageButton
        private lateinit var  btn_Undo: ImageButton
        var eraser_isOn: Boolean = false
        var customProgressDialog :Dialog?=null

val openGalleryLancher : ActivityResultLauncher<Intent> =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult())
        {
            result->
            if(result.resultCode == RESULT_OK && result.data !=  null  ) {
                val imagebackgroound: ImageView = findViewById(R.id.iv_imgondrawing)
           imagebackgroound.setImageURI(result.data?.data)


            }
        }


        val requestPermission: ActivityResultLauncher<Array<String>> =
            registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { permissions ->
                permissions.entries.forEach {
                    val perMissionName = it.key
                    val isGranted = it.value
                    //\ 3: if permission is granted show a toast and perform operation
                    if (isGranted ) {

                        val pickintent=Intent(Intent.ACTION_PICK,MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
                        openGalleryLancher.launch(pickintent)


                    } else {
                        //\ 4: Displaying another toast if permission is not granted and this time focus on
                        //    Read external storage
                        if (perMissionName == Manifest.permission.READ_EXTERNAL_STORAGE) {
                            Toast.makeText(
                                this@MainActivity,
                                "Oops you just denied the permission.",
                                Toast.LENGTH_LONG
                            ).show()
                        }else if (perMissionName == Manifest.permission.WRITE_EXTERNAL_STORAGE)
                        {
                            Toast.makeText(
                                this@MainActivity,
                                "Oops you just denied the permission.",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

            }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        drawingView=findViewById(R.id.Drawing_view)
        drawingView?.setSizeForBrush(20.toFloat())
        btn_ChoseBrushcolor=findViewById(R.id.id_btn_chosecolor)

        btn_Undo=findViewById(R.id.id_btn_undobutton)
        btn_Undo.setOnClickListener {
            drawingView?.undo()
        }


        btn_eraser=findViewById(R.id.id_btn_eraser)
        btn_eraser.setOnClickListener {
            Turneraseron(btn_eraser)
        }

        val ibGallery: ImageButton = findViewById(R.id.ib_gallry)
        //(Step 10 : Adding an click event to image button for selecting the image from gallery.)

        ibGallery.setOnClickListener {
            requestStoragePermission()
        }
val ibSave:ImageButton=findViewById<ImageButton>(R.id.ib_save)
       ibSave.setOnClickListener{

           showProgressDialog()
           if (isReadStorageAllowed()){
               //launch a coroutine block
               lifecycleScope.launch{
                   //reference the frame layout
                   val flDrawingView: FrameLayout = findViewById(R.id.fl_drawing_view_container)
                   //Save the image to the device
                   saveBitmapFile(getBitmapFromView(flDrawingView))
               }
           }else
           {
               requestStoragePermission()
           }
       }




        btn_ChoseBrushcolor.setOnClickListener {
            showBrushcolorChooserDialog()
        }
btn_ChoseBrushsize=findViewById(R.id.id_btn_chosebrushsize)
        btn_ChoseBrushsize.setOnClickListener {

            showBrushSizeChooserDialog()
        }


    }





        fun Turneraseron(btntag:ImageButton)
        {

  if(!eraser_isOn)
  {
      drawingView?.setBrushColor(btntag.tag.toString())

      btntag.setImageDrawable(
          ContextCompat.getDrawable(
              this,
              R.drawable.itemlist
          )
      )
     eraser_isOn=true

  }else if(eraser_isOn)
  {
      if(mImageButtonCurrentPaint!=null)
      {
          drawingView?.setBrushColor(mImageButtonCurrentPaint?.tag.toString())

          btntag.setImageDrawable(null)


      }else {
          drawingView?.setBrushColor("black")

          btntag.setImageDrawable(null)
      }
      eraser_isOn=false
  }





        }
        fun showBrushSizeChooserDialog()
        {
            var brushSizeDialog =Dialog(this)
            brushSizeDialog.setContentView(R.layout.dialog_brush_size)
            //
            brushSizeDialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            var btnsmall=brushSizeDialog.findViewById<ImageButton>(R.id.ib_small_brush)
            btnsmall.setOnClickListener {
                drawingView?.setSizeForBrush(10.toFloat())
                brushSizeDialog.dismiss()
            }

            var btnmed=brushSizeDialog.findViewById<ImageButton>(R.id.ib_medium_brush)
            btnmed.setOnClickListener {
                drawingView?.setSizeForBrush(20.toFloat())
                brushSizeDialog.dismiss()
            }
            var btnslrg=brushSizeDialog.findViewById<ImageButton>(R.id.ib_large_brush)
            btnslrg.setOnClickListener {
                drawingView?.setSizeForBrush(30.toFloat())
                brushSizeDialog.dismiss()
            }

brushSizeDialog.show()
        }

        fun showBrushcolorChooserDialog()
        {
             brushColorDialog =Dialog(this)
            brushColorDialog?.setContentView(R.layout.dialog_brush_color)
            //
            brushColorDialog?.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

            if(mImageButtonCurrentPaint==null) {
                var btndefault = brushColorDialog!!.findViewById<ImageButton>(R.id.id_btn_black)
                btndefault.setImageDrawable(
                    ContextCompat.getDrawable(
                        this,
                        R.drawable.itemlistpressed
                    )
                )
                val tag =btndefault.tag.toString()
                mImageButtonCurrentPaint=btndefault
                Log.i("main","  brush is $mImageButtonCurrentPaint")
            }

            brushColorDialog?.show()
        }


        fun PaintColorClocked(view : View){
           if(mImageButtonCurrentPaint!=null)
           {

               val imageButton = view as ImageButton
               // Here the tag is used for swaping the current color with previous color.
               // The tag stores the selected view
               val colorTag = imageButton.tag.toString()
               Log.i("main","name = ${imageButton}")
               Log.i("main","colortag = ${colorTag}")
               // The color is set as per the selected tag here.

                drawingView?.setBrushColor(colorTag)

               // Swap the backgrounds for last active and currently active image button.
               imageButton.setImageDrawable(ContextCompat.getDrawable(this, R.drawable.itemlistpressed))
               mImageButtonCurrentPaint?.setImageDrawable(
                   ContextCompat.getDrawable(
                       this,
                       R.drawable.itemlist
                   )
               )

               //Current view is updated with selected view in the form of ImageButton.
               mImageButtonCurrentPaint = view
               brushColorDialog?.dismiss()
           }

        }

        private fun showRationaleDialog(
            title: String,
            message: String,
        ) {
            val builder: androidx.appcompat.app.AlertDialog.Builder = androidx.appcompat.app.AlertDialog.Builder(this)
            builder.setTitle(title)
                .setMessage(message)
                .setPositiveButton("Cancel") { dialog, _ ->
                    dialog.dismiss()
                }
            builder.create().show()
        }


        private fun isReadStorageAllowed(): Boolean {
            //Getting the permission status
            // Here the checkSelfPermission is
            /**
             * Determine whether <em>you</em> have been granted a particular permission.
             *
             * @param permission The name of the permission being checked.
             *
             */
            val result = ContextCompat.checkSelfPermission(
                this, Manifest.permission.READ_EXTERNAL_STORAGE
            )

            /**
             *
             * @return {@link android.content.pm.PackageManager#PERMISSION_GRANTED} if you have the
             * permission, or {@link android.content.pm.PackageManager#PERMISSION_DENIED} if not.
             *
             */
            //If permission is granted returning true and If permission is not granted returning false
            return result == PackageManager.PERMISSION_GRANTED
        }
    private fun requestStoragePermission(){
        //To 6: Check if the permission was denied and show rationale
        if (
            ActivityCompat.shouldShowRequestPermissionRationale(
                this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
        ){
            // 9: call the rationale dialog to tell the user why they need to allow permission request
            showRationaleDialog("Kids Drawing App","Kids Drawing App " +
                    "needs to Access Your External Storage")
        }
        else {
            // You can directly ask for the permission.
            // To 7: if it has not been denied then request for permission
            //  The registered ActivityResultCallback gets the result of this request.
            requestPermission.launch(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                )
            )
        }

    }

        private fun getBitmapFromView(view: View): Bitmap {

            //Define a bitmap with the same size as the view.
            // CreateBitmap : Returns a mutable bitmap with the specified width and height
            val returnedBitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
            //Bind a canvas to it
            val canvas = Canvas(returnedBitmap)
            //Get the view's background
            val bgDrawable = view.background
            if (bgDrawable != null) {
                //has background drawable, then draw it on the canvas
                bgDrawable.draw(canvas)
            } else {
                //does not have background drawable, then draw white background on the canvas
                canvas.drawColor(Color.WHITE)
            }
            // draw the view on the canvas
            view.draw(canvas)
            //return the bitmap
            return returnedBitmap
        }


        private  suspend fun saveBitmapFile(bitmap : Bitmap?) : String
        { var result =""
            withContext(Dispatchers.IO){
                if(bitmap!=null)
                {
                 try{
                     val bytes = ByteArrayOutputStream()
                     bitmap.compress(Bitmap.CompressFormat.PNG,90,bytes)
                     
                     val f= File(externalCacheDir?.absoluteFile.toString()
                             +File.separator + "KidDrawingApp_" + System.currentTimeMillis()
                     )


                     val fName = File(getExternalFilesDir(null), "MyPaint_" + (System.currentTimeMillis()/1000) + ".png")

                     val fo=FileOutputStream(f)
                     fo.write(bytes.toByteArray())
                     fo.close()
                     result=f.absolutePath
                     Log.e("File Selector",
                         "The selected file can't be shared: $fName")
                     Log.e("File Selector",
                         "The selected file can't be shared: $f")
                     Log.e("File Selector",
                         "The selected file can't be shared: $result")
                     val fileUri: Uri? = try {
                         FileProvider.getUriForFile(
                             applicationContext, "com.example.drawingapp10.fileprovider",
                             f)

                     } catch (e: IllegalArgumentException) {
                         Log.e("File Selector",
                             "The selected file can't be shared: $fName")
                         null
                     }
                     Log.e("File Selector",
                         "The selected file can't be shared: $fileUri")


                     runOnUiThread {
                         if (!result.isEmpty()) {
                             canceldialo()
                             Toast.makeText(
                                 this@MainActivity,
                                 "File saved successfully :$result",
                                 Toast.LENGTH_SHORT
                             ).show()
                           //  shareimagenew()
                             shareImage(result)
                        //  setUpEnablingFeatures(FileProvider.getUriForFile(applicationContext,"com.example.drawingapp10.fileprovider",fName))
                         } else {
                             Toast.makeText(
                                 this@MainActivity,
                                 "Something went wrong while saving the file.",
                                 Toast.LENGTH_SHORT
                             ).show()
                         }
                     }
                 } catch (e: Exception) {
                     result = ""
                     e.printStackTrace()
                 }
                }
            }
            return result
        }

private  fun showProgressDialog()
{
    customProgressDialog=Dialog(this)

customProgressDialog?.setContentView(R.layout.progressdialog)
    customProgressDialog?.show()


}

        private  fun canceldialo()
        {

            if(customProgressDialog!=null)
            {
                customProgressDialog?.dismiss()
                customProgressDialog=null
            }
        }

        private fun shareImage(result: String )
        {

            MediaScannerConnection.scanFile(this,arrayOf(result),null)
            {
                path, uri->
                val shareintent =Intent()
                shareintent.action=Intent.ACTION_SEND
                shareintent.putExtra(Intent.EXTRA_STREAM,uri)
                shareintent.type="image/png"
                startActivity(Intent.createChooser(shareintent,"Share"))
            }
        }
        private fun setUpEnablingFeatures(uri: Uri){


            val intent = Intent().apply {
                intent.action = Intent.ACTION_SEND
                intent.putExtra(Intent.EXTRA_STREAM, uri)
                intent.type = "image/png"
            }
            startActivity(Intent.createChooser(intent, "Share image via "))

        }



        private fun shareimagenew()
        {


            val requestFile = File(externalCacheDir?.absoluteFile.toString() + File.separator )
            val fileUri: Uri? = try {
                FileProvider.getUriForFile(
                    applicationContext, "com.example.drawingapp10.fileprovider",
                    requestFile)
            } catch (e: IllegalArgumentException) {
                Log.e("File Selector",
                    "The selected file can't be shared: $requestFile")
                null
            }


            val shareIntent = Intent()
            shareIntent.action = Intent.ACTION_SEND
            shareIntent.type = "image/png"
            shareIntent.putExtra(Intent.EXTRA_STREAM, fileUri)

            startActivity(
                Intent.createChooser(
                    shareIntent, "Share"
                )
            )
        }

    }
package com.example.AgendaLimpio.Fotos

import android.Manifest
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.Observer
import androidx.recyclerview.widget.GridLayoutManager
import com.example.AgendaLimpio.Data.DataBase.AppDatabase
import com.example.AgendaLimpio.Data.Model.PedidoFoto
import com.example.AgendaLimpio.Fotos.Adapter.FotosAdapter
import com.example.AgendaLimpio.R
import com.example.AgendaLimpio.databinding.ActivityFotosBinding
import java.io.File

class FotosActivity : AppCompatActivity() {
    private lateinit var binding: ActivityFotosBinding
    private lateinit var photoAdapter: FotosAdapter
    private var tempPhotoFile: File? = null

    private val viewModel: FotosViewModel by viewModels {
        FotosViewModelFactory(application, AppDatabase.getDatabase(this).pedidoPhotoDao())
    }

    private val requestPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) {
            viewModel.onAddPhotoClicked() // El ViewModel prepara el fichero y URI
        } else {
            Toast.makeText(this, "Permiso de cámara denegado.", Toast.LENGTH_SHORT).show()
        }
    }

    private val takePictureLauncher = registerForActivityResult(ActivityResultContracts.TakePicture()) { success ->
        if (success) {
            tempPhotoFile?.let { viewModel.onPhotoTaken(it) }
        }
    }

    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        uri?.let { viewModel.onPhotoSelectedFromGallery(it) }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFotosBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val pedidoId = intent.getStringExtra("EXTRA_PEDIDO_ID")
        val photoType = intent.getStringExtra("EXTRA_PHOTO_TYPE")

        if (pedidoId == null || photoType == null) {
            Toast.makeText(this, "Error: Faltan datos del pedido.", Toast.LENGTH_LONG).show()
            finish()
            return
        }

        setupUI(photoType)
        setupClickListeners()
        observeViewModel()

        viewModel.initialize(pedidoId, photoType)
    }

    private fun setupUI(photoType: String) {
        supportActionBar?.title = "Fotos de Pedido ($photoType)"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        photoAdapter = FotosAdapter(emptyList()) { photo ->
            viewModel.onDeletePhotoClicked(photo)
        }
        binding.recyclerViewPhotos.adapter = photoAdapter
        binding.recyclerViewPhotos.layoutManager = GridLayoutManager(this, 2)
    }

    private fun setupClickListeners() {
        binding.fabAddPhoto.setOnClickListener {
            showPhotoSourceDialog()
        }
    }

    private fun observeViewModel() {
        viewModel.uiState.observe(this, Observer { state ->
            // binding.progressBar.visibility = if (state.isLoading) View.VISIBLE else View.GONE
            if (state.photos.isEmpty()) {
                binding.textViewEmpty.visibility = View.VISIBLE
                binding.recyclerViewPhotos.visibility = View.GONE
            } else {
                binding.textViewEmpty.visibility = View.GONE
                binding.recyclerViewPhotos.visibility = View.VISIBLE
                photoAdapter.updatePhotos(state.photos)
            }
        })

        viewModel.events.observe(this, Observer { event ->
            when (event) {
                is FotosEvent.ShowToast -> Toast.makeText(this, event.message, Toast.LENGTH_SHORT)
                    .show()

                is FotosEvent.ShowDeleteConfirmation -> showDeleteConfirmationDialog(event.photo)
                is FotosEvent.LaunchCamera -> {
                    this.tempPhotoFile = event.tempFile
                    takePictureLauncher.launch(event.photoFileUri)
                }

                is FotosEvent.LaunchGallery -> pickImageLauncher.launch("image/*")
            }
        })
    }

    private fun showPhotoSourceDialog() {
        val options = arrayOf("Abrir Cámara", "Elegir de la Galería")
        AlertDialog.Builder(this)
            .setTitle("Añadir Foto")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> checkCameraPermission()
                    1 -> viewModel.onLaunchGalleryClicked()
                }
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    private fun checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            viewModel.onAddPhotoClicked()
        } else {
            requestPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    private fun showDeleteConfirmationDialog(photo: PedidoFoto) {
        AlertDialog.Builder(this)
            .setTitle("Confirmar Borrado")
            .setMessage("¿Estás seguro de que quieres borrar esta foto?")
            .setPositiveButton("Borrar") { _, _ ->
                viewModel.onDeletePhotoConfirmed(photo)
            }
            .setNegativeButton("Cancelar", null)
            .setIcon(R.drawable.ic_delete)
            .show()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }
}
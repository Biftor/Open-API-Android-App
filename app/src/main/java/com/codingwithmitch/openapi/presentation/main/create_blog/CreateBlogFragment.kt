package com.codingwithmitch.openapi.presentation.main.create_blog

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.*
import androidx.fragment.app.viewModels
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.codingwithmitch.openapi.R
import com.codingwithmitch.openapi.business.domain.util.*
import com.codingwithmitch.openapi.business.domain.util.Constants.Companion.GALLERY_REQUEST_CODE
import com.codingwithmitch.openapi.business.domain.util.ErrorHandling.Companion.ERROR_SOMETHING_WRONG_WITH_IMAGE
import com.codingwithmitch.openapi.databinding.FragmentCreateBlogBinding
import com.codingwithmitch.openapi.presentation.util.processQueue
import com.theartofdev.edmodo.cropper.CropImage
import com.theartofdev.edmodo.cropper.CropImageView

class CreateBlogFragment : BaseCreateBlogFragment()
{

    private val requestOptions = RequestOptions
        .placeholderOf(R.drawable.default_image)
        .error(R.drawable.default_image)

    private val viewModel: CreateBlogViewModel by viewModels()

    private var _binding: FragmentCreateBlogBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentCreateBlogBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setHasOptionsMenu(true)

        binding.blogImage.setOnClickListener {
            if(uiCommunicationListener.isStoragePermissionGranted()){
                pickFromGallery()
            }
        }

        binding.updateTextview.setOnClickListener {
            if(uiCommunicationListener.isStoragePermissionGranted()){
                pickFromGallery()
            }
        }

        subscribeObservers()
    }

    private fun pickFromGallery() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        val mimeTypes = arrayOf("image/jpeg", "image/png", "image/jpg")
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes)
        intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivityForResult(intent, GALLERY_REQUEST_CODE)
    }

    fun subscribeObservers(){
        viewModel.state.observe(viewLifecycleOwner, { state ->
            uiCommunicationListener.displayProgressBar(state.isLoading)
            processQueue(
                context = context,
                queue = state.queue,
                stateMessageCallback = object: StateMessageCallback {
                    override fun removeMessageFromStack() {
                        viewModel.onTriggerEvent(CreateBlogEvents.OnRemoveHeadFromQueue)
                    }
                })
            setBlogProperties(
                title = state.title,
                body = state.body,
                uri = state.uri,
            )
            if(state.onPublishSuccess){
                findNavController().popBackStack(R.id.blogFragment, false)
            }
        })
    }

    fun setBlogProperties(
        title: String,
        body: String,
        uri: Uri?
    ){
        if(uri != null){
            Glide.with(this)
                .setDefaultRequestOptions(requestOptions)
                .load(uri)
                .into(binding.blogImage)
        }
        else{
            Glide.with(this)
                .setDefaultRequestOptions(requestOptions)
                .load(R.drawable.default_image)
                .into(binding.blogImage)
        }

        binding.blogTitle.setText(title)
        binding.blogBody.setText(body)
    }


    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK) {
            Log.d(TAG, "CROP: RESULT OK")
            when (requestCode) {

                GALLERY_REQUEST_CODE -> {
                    data?.data?.let { uri ->
                        activity?.let{
                            launchImageCrop(uri)
                        }
                    }?: showErrorDialog(ERROR_SOMETHING_WRONG_WITH_IMAGE)
                }

                CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE -> {
                    Log.d(TAG, "CROP: CROP_IMAGE_ACTIVITY_REQUEST_CODE")
                    val result = CropImage.getActivityResult(data)
                    val resultUri = result.uri
                    Log.d(TAG, "CROP: CROP_IMAGE_ACTIVITY_REQUEST_CODE: uri: ${resultUri}")
                    viewModel.onTriggerEvent(CreateBlogEvents.OnUpdateUri(resultUri))
                }

                CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE -> {
                    Log.d(TAG, "CROP: ERROR")
                    showErrorDialog(ERROR_SOMETHING_WRONG_WITH_IMAGE)
                }
            }
        }
    }

    private fun showErrorDialog(message: String){
        viewModel.onTriggerEvent(CreateBlogEvents.Error(
            stateMessage = StateMessage(
                response = Response(
                    message = message,
                    uiComponentType = UIComponentType.Dialog(),
                    messageType = MessageType.Error()
                )
            )
        ))
    }

    private fun launchImageCrop(uri: Uri){
        context?.let{
            CropImage.activity(uri)
                .setGuidelines(CropImageView.Guidelines.ON)
                .start(it, this)
        }
    }

    private fun cacheState(){
        val title = binding.blogTitle.text.toString()
        val body = binding.blogBody.text.toString()
        viewModel.onTriggerEvent(CreateBlogEvents.OnUpdateTitle(title))
        viewModel.onTriggerEvent(CreateBlogEvents.OnUpdateBody(body))
    }

    override fun onPause() {
        super.onPause()
        cacheState()
    }

    override fun onCreateOptionsMenu(menu: Menu, inflater: MenuInflater) {
        inflater.inflate(R.menu.publish_menu, menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when(item.itemId){
            R.id.publish -> {
                cacheState()
                viewModel.onTriggerEvent(CreateBlogEvents.PublishBlog)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}











package es.uc3m.android.chillmates.user

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import es.uc3m.android.chillmates.R

@Immutable
data class FlatMemberUi(
    val id: String,
    val name: String
)

@Immutable
data class UserUiState(
    val fullName: String = "",
    val email: String = "",
    val draftFullName: String = "",
    val draftEmail: String = "",
    val isProfileEditing: Boolean = false,
    val flatName: String = "",
    val members: List<FlatMemberUi> = emptyList(),
    val profilePhotoUrl: String? = null,
    val isFlatNameEditing: Boolean = false
)

class UserViewModel : ViewModel() {
    // TODO: read from DB
    var uiState by mutableStateOf(
        UserUiState(
            fullName = "Marta",
            email = "marta@email.com",
            draftFullName = "Marta",
            draftEmail = "marta@email.com",
            isProfileEditing = false,
            flatName = "ChillMates Flat",
            members = listOf(
                FlatMemberUi("m1", "Marta"),
                FlatMemberUi("m2", "Lorena"),
                FlatMemberUi("m3", "Fran")
            ),
            profilePhotoUrl = null,
            isFlatNameEditing = false
        )
    )
        private set

    /**
     * Enters edit mode by copying current values into draft state.
     */
    fun onEditProfileClick() {
        uiState = uiState.copy(
            isProfileEditing = true,
            draftFullName = uiState.fullName,
            draftEmail = uiState.email
        )
    }

    /**
     * Finalizes changes by moving draft values into the primary state.
     */
    fun onSaveProfileClick() {
        uiState = uiState.copy(
            isProfileEditing = false,
            fullName = uiState.draftFullName,
            email = uiState.draftEmail
        )
    }

    fun onDraftNameChange(newValue: String) {
        uiState = uiState.copy(draftFullName = newValue)
    }

    fun onDraftEmailChange(newValue: String) {
        uiState = uiState.copy(draftEmail = newValue)
    }

    fun onFlatNameChange(newValue: String) {
        uiState = uiState.copy(flatName = newValue)
    }

    /**
     * Toggles between read-only and editable modes for the flat name.
     */
    fun onToggleFlatNameEditing() {
        uiState = uiState.copy(isFlatNameEditing = !uiState.isFlatNameEditing)
    }

    fun onChangePasswordClick() {
        // TODO: Navigation to password reset or change password dialog
    }

    /**
     * Updates the profile photo reference with a new URI.
     */
    fun onChangePhotoClick(uri: Uri) {
        uiState = uiState.copy(profilePhotoUrl = uri.toString())
    }

    /**
     * Triggers a system share sheet to invite new members to the current flat.
     */
    fun onInviteClick(context: Context) {
        val inviteMsg = "Join my flat on ChillMates! Click here: https://chillmates.app/join/flat123"
        val chooserTitle = context.getString(R.string.user_invite_chooser_title)

        val intent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, inviteMsg)
        }
        val chooser = Intent.createChooser(intent, chooserTitle)
        context.startActivity(chooser)
    }

    fun onLeaveFlatClick() {
        // TODO: Implement logic to remove current user from the flat group
    }

    fun onCreateNewFlatClick() {
        // TODO: Implementation for creating a new flat entry or joining via code
    }
}
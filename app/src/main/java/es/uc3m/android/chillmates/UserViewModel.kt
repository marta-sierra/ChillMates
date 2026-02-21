package es.uc3m.android.chillmates

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel

// individual roommate
@Immutable
data class FlatMemberUi(
    val id: String,
    val name: String
)

// settings state
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

    fun onEditProfileClick() {
        // "Edit my info" button
        uiState = uiState.copy(
            isProfileEditing = true,
            draftFullName = uiState.fullName,
            draftEmail = uiState.email
        )
    }

    fun onSaveProfileClick() {
        // "Save changes" button
        uiState = uiState.copy(
            isProfileEditing = false,
            fullName = uiState.draftFullName,
            email = uiState.draftEmail
        )
    }

    fun onDraftNameChange(newValue: String) {
        // Editing name value
        uiState = uiState.copy(draftFullName = newValue)
    }

    fun onDraftEmailChange(newValue: String) {
        // Editing email value
        uiState = uiState.copy(draftEmail = newValue)
    }

    fun onFlatNameChange(newValue: String) {
        // Editing flat name value
        uiState = uiState.copy(flatName = newValue)
    }

    fun onToggleFlatNameEditing() {
        // Changes between blocked/editable. Manages item change
        uiState = uiState.copy(isFlatNameEditing = !uiState.isFlatNameEditing)
    }

    fun onChangePasswordClick() {
        // "Password" button
    }
    fun onChangePhotoClick() {
        // "Change photo" button
    }
    fun onInviteClick() {
        // "Invite roommate" button
    }
    fun onLeaveFlatClick() {
        // "Leave flat" button
    }
    fun onCreateNewFlatClick() {
        // "Create/join a new flat" button
    }
}
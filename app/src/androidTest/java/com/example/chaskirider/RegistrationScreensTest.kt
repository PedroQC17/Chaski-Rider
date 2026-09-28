package com.example.chaskirider

import android.graphics.Bitmap
import androidx.activity.ComponentActivity
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.test.*
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.test.platform.app.InstrumentationRegistry
import com.example.chaskirider.domain.model.*
import com.example.chaskirider.ui.screens.auth.AccessScreen
import com.example.chaskirider.ui.screens.onboarding.OnboardingStep3Screen
import com.example.chaskirider.ui.theme.ChaskiRiderTheme
import org.junit.Rule
import org.junit.Test
import java.io.File

class RegistrationScreensTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()
    @Test fun accessHasRealButtonsAndDisabledApple() {
        compose.setContent { ChaskiRiderTheme { AccessScreen() } }
        compose.onNodeWithText("Continuar con Google").assertIsEnabled()
        compose.onNodeWithText("Continuar con Apple").assertIsNotEnabled()
        compose.onNodeWithText("Ingresar con correo").performScrollTo().assertIsEnabled()
        compose.onNodeWithText("Ingresar con celular").assertDoesNotExist()
        val bitmap = compose.onRoot().captureToImage().asAndroidBitmap()
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        File(context.filesDir, "access-screen.png").outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
    }
    @Test fun cannotSubmitMissingMotorcycleDocuments() {
        compose.setContent { ChaskiRiderTheme {
            OnboardingStep3Screen(vehicleType = VehicleType.MOTORCYCLE,
                initialBankInfo = BankInfo("BCP", "Ana", "12345678", ""))
        } }
        compose.onNodeWithText("Licencia de conducir").assertExists()
        compose.onNodeWithText("SOAT").assertExists()
        compose.onNodeWithText("Finalizar registro").performScrollTo().assertIsNotEnabled()
    }
    @Test fun bicycleDoesNotRequireMotorcycleDocuments() {
        val docs = RegistrationValidation.requiredDocuments(VehicleType.BICYCLE)
            .associateWith { DocumentFile(id = it, uploadState = DocumentUploadState.UPLOADED) }
        compose.setContent { ChaskiRiderTheme {
            OnboardingStep3Screen(vehicleType = VehicleType.BICYCLE,
                initialBankInfo = BankInfo("BCP", "Ana", "12345678", ""), documentsMap = docs)
        } }
        compose.onNodeWithText("Licencia de conducir").assertDoesNotExist()
        compose.onNodeWithText("SOAT").assertDoesNotExist()
        compose.onNodeWithText("Finalizar registro").performScrollTo().assertIsEnabled()
    }
}

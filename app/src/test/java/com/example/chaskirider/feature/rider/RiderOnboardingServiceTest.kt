// Parte 1 - Pruebas de validación actualizadas a las reglas vigentes tras el
// refactor MVVM: contraseña obligatoria antes del vehículo, automóvil fuera del
// registro (solo lectura de perfiles antiguos), foto de perfil requerida para
// activarse y catálogo de bancos BCP/INTERBANK/BBVA.

package com.example.chaskirider.feature.rider
import com.example.chaskirider.feature.rider.domain.Rider
import com.example.chaskirider.feature.rider.domain.RiderStatus
import com.example.chaskirider.feature.rider.domain.RiderPolicy
import com.example.chaskirider.feature.rider.domain.RiderOnboardingService
import com.example.chaskirider.feature.bank.domain.BankAccount
import com.example.chaskirider.feature.bank.domain.BankPolicy
import com.example.chaskirider.feature.rider.domain.VehicleType
import com.example.chaskirider.feature.rider.domain.VehiclePolicy
import org.junit.Assert.*
import org.junit.Test

class RiderOnboardingServiceTest {
    @Test fun phoneNormalizationDoesNotDuplicateCountryCode() {
        assertEquals("+51987654321", RiderPolicy.normalizePhone("987654321"))
        assertEquals("+51987654321", RiderPolicy.normalizePhone("+51 987-654-321"))
    }
    @Test fun incompletePersonalDataTakesPriorityOverStoredStep() {
        assertEquals(1, RiderOnboardingService.nextStep(Rider(currentStep = 3, hasPassword = true)))
        val personal = Rider(name = "Ana", lastName = "Perez", dni = "00123456", phone = "+51987654321", termsAccepted = true, hasPassword = true)
        assertEquals(1, RiderOnboardingService.nextStep(personal.copy(hasPassword = false)))
        assertEquals(2, RiderOnboardingService.nextStep(personal))
        assertEquals(3, RiderOnboardingService.nextStep(personal.copy(vehicleTypeId = VehicleType.BICYCLE.name)))
    }
    @Test fun motorcycleRequiresLicenseAndSoat() {
        assertEquals(3, VehiclePolicy.requiredDocuments(VehicleType.BICYCLE).size)
        assertTrue(VehiclePolicy.requiredDocuments(VehicleType.MOTORCYCLE).containsAll(listOf("driverLicense", "soat")))
    }
    @Test fun carIsNotSelectableAndKeepsLegacyDocumentList() {
        val personal = Rider(name = "Ana", lastName = "Perez", dni = "00123456", phone = "+51987654321", termsAccepted = true, hasPassword = true)
        assertEquals(2, RiderOnboardingService.nextStep(personal.copy(vehicleTypeId = VehicleType.CAR.name)))
        assertEquals(3, VehiclePolicy.requiredDocuments(VehicleType.CAR).size)
    }
    @Test fun onlyEnabledRidersCanGoAvailable() {
        val approved = Rider(status = RiderStatus.APPROVED, isEnabled = true, profilePhotoPath = "photo.jpg")
        assertNull(RiderPolicy.availabilityError(approved, activating = true))
        assertNull(RiderPolicy.availabilityError(approved.copy(isEnabled = false), activating = false))
        assertNotNull(RiderPolicy.availabilityError(approved.copy(isEnabled = false), activating = true))
        assertNotNull(RiderPolicy.availabilityError(approved.copy(status = RiderStatus.PENDING_REVIEW), activating = true))
    }
    @Test fun availabilityRequiresProfilePhoto() {
        val approved = Rider(status = RiderStatus.APPROVED, isEnabled = true)
        assertNotNull(RiderPolicy.availabilityError(approved, activating = true))
        assertNull(RiderPolicy.availabilityError(approved, activating = false))
    }
    @Test fun invalidDniAndMissingConsentAreRejected() {
        assertNotNull(RiderPolicy.personalError("Ana", "Perez", "123", "987654321", true))
        assertNotNull(RiderPolicy.personalError("Ana", "Perez", "00123456", "987654321", false))
        assertNull(RiderPolicy.personalError("Ana", "Perez", "00123456", "987654321", true))
    }
    @Test fun bankAllowsAccountOrValidCci() {
        assertNotNull(BankPolicy.error(BankAccount("BCP", "Ana", "", "")))
        assertNotNull(BankPolicy.error(BankAccount("BCP", "Ana", "", "123")))
        assertNull(BankPolicy.error(BankAccount("BCP", "Ana", "", "00123456789012345678")))
    }
    @Test fun unknownBankIsRejected() {
        assertNotNull(BankPolicy.error(BankAccount("Bank", "Ana", "", "00123456789012345678")))
    }
}

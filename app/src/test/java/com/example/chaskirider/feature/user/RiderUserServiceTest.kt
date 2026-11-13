// Parte 1 - Pruebas de validación actualizadas a las reglas vigentes tras el
// refactor MVVM: contraseña obligatoria antes del vehículo, automóvil fuera del
// registro (solo lectura de perfiles antiguos), foto de perfil requerida para
// activarse y catálogo de bancos BCP/INTERBANK/BBVA.

package com.example.chaskirider.feature.user
import com.example.chaskirider.feature.user.domain.RiderUser
import com.example.chaskirider.feature.user.domain.RegistrationStatus
import com.example.chaskirider.feature.user.domain.RiderUserPolicy
import com.example.chaskirider.feature.user.domain.RiderUserService
import com.example.chaskirider.feature.bank.domain.BankInfo
import com.example.chaskirider.feature.bank.domain.BankPolicy
import com.example.chaskirider.feature.vehicle.domain.VehicleType
import com.example.chaskirider.feature.vehicle.domain.VehiclePolicy
import org.junit.Assert.*
import org.junit.Test

class RiderUserServiceTest {
    @Test fun phoneNormalizationDoesNotDuplicateCountryCode() {
        assertEquals("+51987654321", RiderUserPolicy.normalizePhone("987654321"))
        assertEquals("+51987654321", RiderUserPolicy.normalizePhone("+51 987-654-321"))
    }
    @Test fun incompletePersonalDataTakesPriorityOverStoredStep() {
        assertEquals(1, RiderUserService.nextStep(RiderUser(currentStep = 3, hasPassword = true)))
        val personal = RiderUser(name = "Ana", lastName = "Perez", dni = "00123456", phone = "+51987654321", termsAccepted = true, hasPassword = true)
        assertEquals(1, RiderUserService.nextStep(personal.copy(hasPassword = false)))
        assertEquals(2, RiderUserService.nextStep(personal))
        assertEquals(3, RiderUserService.nextStep(personal.copy(vehicleTypeId = VehicleType.BICYCLE.name)))
    }
    @Test fun motorcycleRequiresLicenseAndSoat() {
        assertEquals(3, VehiclePolicy.requiredDocuments(VehicleType.BICYCLE).size)
        assertTrue(VehiclePolicy.requiredDocuments(VehicleType.MOTORCYCLE).containsAll(listOf("driverLicense", "soat")))
    }
    @Test fun carIsNotSelectableAndKeepsLegacyDocumentList() {
        val personal = RiderUser(name = "Ana", lastName = "Perez", dni = "00123456", phone = "+51987654321", termsAccepted = true, hasPassword = true)
        assertEquals(2, RiderUserService.nextStep(personal.copy(vehicleTypeId = VehicleType.CAR.name)))
        assertEquals(3, VehiclePolicy.requiredDocuments(VehicleType.CAR).size)
    }
    @Test fun onlyEnabledRidersCanGoAvailable() {
        val approved = RiderUser(status = RegistrationStatus.APPROVED, isEnabled = true, profilePhotoPath = "photo.jpg")
        assertNull(RiderUserPolicy.availabilityError(approved, activating = true))
        assertNull(RiderUserPolicy.availabilityError(approved.copy(isEnabled = false), activating = false))
        assertNotNull(RiderUserPolicy.availabilityError(approved.copy(isEnabled = false), activating = true))
        assertNotNull(RiderUserPolicy.availabilityError(approved.copy(status = RegistrationStatus.PENDING_REVIEW), activating = true))
    }
    @Test fun availabilityRequiresProfilePhoto() {
        val approved = RiderUser(status = RegistrationStatus.APPROVED, isEnabled = true)
        assertNotNull(RiderUserPolicy.availabilityError(approved, activating = true))
        assertNull(RiderUserPolicy.availabilityError(approved, activating = false))
    }
    @Test fun invalidDniAndMissingConsentAreRejected() {
        assertNotNull(RiderUserPolicy.personalError("Ana", "Perez", "123", "987654321", true))
        assertNotNull(RiderUserPolicy.personalError("Ana", "Perez", "00123456", "987654321", false))
        assertNull(RiderUserPolicy.personalError("Ana", "Perez", "00123456", "987654321", true))
    }
    @Test fun bankAllowsAccountOrValidCci() {
        assertNotNull(BankPolicy.error(BankInfo("BCP", "Ana", "", "")))
        assertNotNull(BankPolicy.error(BankInfo("BCP", "Ana", "", "123")))
        assertNull(BankPolicy.error(BankInfo("BCP", "Ana", "", "00123456789012345678")))
    }
    @Test fun unknownBankIsRejected() {
        assertNotNull(BankPolicy.error(BankInfo("Bank", "Ana", "", "00123456789012345678")))
    }
}

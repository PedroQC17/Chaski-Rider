package com.example.chaskirider

import com.example.chaskirider.domain.model.*
import org.junit.Assert.*
import org.junit.Test

class RegistrationValidationTest {
    @Test fun phoneNormalizationDoesNotDuplicateCountryCode() {
        assertEquals("+51987654321", RegistrationValidation.normalizePhone("987654321"))
        assertEquals("+51987654321", RegistrationValidation.normalizePhone("+51 987-654-321"))
    }
    @Test fun incompletePersonalDataTakesPriorityOverStoredStep() {
        assertEquals(1, RegistrationValidation.nextStep(RiderUser(currentStep = 3, hasPassword = true)))
        val personal = RiderUser(name = "Ana", lastName = "Perez", dni = "00123456", phone = "+51987654321", termsAccepted = true, hasPassword = true)
        assertEquals(1, RegistrationValidation.nextStep(personal.copy(hasPassword = false)))
        assertEquals(2, RegistrationValidation.nextStep(personal))
        assertEquals(3, RegistrationValidation.nextStep(personal.copy(vehicleType = VehicleType.BICYCLE)))
    }
    @Test fun motorcycleRequiresLicenseAndSoat() {
        assertEquals(3, RegistrationValidation.requiredDocuments(VehicleType.BICYCLE).size)
        assertTrue(RegistrationValidation.requiredDocuments(VehicleType.MOTORCYCLE).containsAll(listOf("driverLicense", "soat")))
    }
    @Test fun carIsNotSelectableAndKeepsLegacyDocumentList() {
        val personal = RiderUser(name = "Ana", lastName = "Perez", dni = "00123456", phone = "+51987654321", termsAccepted = true, hasPassword = true)
        assertEquals(2, RegistrationValidation.nextStep(personal.copy(vehicleType = VehicleType.CAR)))
        assertEquals(3, RegistrationValidation.requiredDocuments(VehicleType.CAR).size)
    }
    @Test fun onlyEnabledRidersCanGoAvailable() {
        val approved = RiderUser(status = RegistrationStatus.APPROVED, isEnabled = true)
        assertNull(RegistrationValidation.availabilityError(approved, activating = true))
        assertNull(RegistrationValidation.availabilityError(approved.copy(isEnabled = false), activating = false))
        assertNotNull(RegistrationValidation.availabilityError(approved.copy(isEnabled = false), activating = true))
        assertNotNull(RegistrationValidation.availabilityError(RiderUser(status = RegistrationStatus.PENDING_REVIEW), activating = true))
    }
    @Test fun invalidDniAndMissingConsentAreRejected() {
        assertNotNull(RegistrationValidation.personalError("Ana", "Perez", "123", "987654321", true))
        assertNotNull(RegistrationValidation.personalError("Ana", "Perez", "00123456", "987654321", false))
        assertNull(RegistrationValidation.personalError("Ana", "Perez", "00123456", "987654321", true))
    }
    @Test fun bankAllowsAccountOrValidCci() {
        assertNotNull(RegistrationValidation.bankError(BankInfo("Bank", "Ana", "", "")))
        assertNotNull(RegistrationValidation.bankError(BankInfo("Bank", "Ana", "", "123")))
        assertNull(RegistrationValidation.bankError(BankInfo("Bank", "Ana", "", "00123456789012345678")))
    }
}

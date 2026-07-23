package com.personalbookkeeping.domain.model

import org.junit.Assert.assertEquals
import org.junit.Test

class AccountTypeTest {
    @Test
    fun legacyDevelopmentValuesMapToSignedSchemaValues() {
        assertEquals(AccountType.CREDIT_CARD, AccountType.fromStoredValue("CREDIT"))
        assertEquals(AccountType.OTHER, AccountType.fromStoredValue("INVESTMENT"))
        assertEquals(AccountType.E_WALLET, AccountType.fromStoredValue("E_WALLET"))
    }
}

package dev.hotwire.core.turbo.errors

import org.junit.Assert.assertEquals
import org.junit.Test

class VisitErrorTest {
    @Test
    @Suppress("DEPRECATION")
    fun `the deprecated description function still resolves to the property`() {
        val errors: List<VisitError> = listOf(
            LoadError.NotPresent,
            HttpError.ClientError.NotFound,
            HttpError.UnknownError(599, null),
            WebError.Unknown,
            WebSslError.NotYetValid
        )

        errors.forEach { assertEquals(it.description, it.description()) }
    }
}

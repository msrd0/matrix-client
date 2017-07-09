package msrd0.matrix.client.test

import msrd0.matrix.client.*
import java.net.URI
import org.hamcrest.MatcherAssert.*
import org.hamcrest.Matchers.*
import org.junit.*
import org.junit.rules.ExpectedException

class ClientTest
{
	companion object
	{
		val hs = HomeServer("matrix.org", URI("https://matrix.org"))
		val id = MatrixID("msrd0", "matrix.org")
		val context = Context(hs, id)
	}
	
	@Rule
	@JvmField
	val exceptionGrabber = ExpectedException.none()
	
	@Test
	fun client_auth()
	{
		val client = Client(context)
		val authList = client.auth()
		assertThat(authList.size, greaterThan(0))
		val auth = authList.first()
		exceptionGrabber.expect(MatrixErrorResponseException::class.java)
		auth.submit()
	}
}

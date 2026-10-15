package org.andrew.enzona.feature.friends

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.andrew.enzona.core.logging.LogLevel
import org.andrew.enzona.core.logging.withLogging
import org.andrew.enzona.core.result.EzResult
import org.andrew.enzona.core.result.Failure
import org.andrew.enzona.feature.account.domain.GetAccountUseCase
import org.andrew.enzona.feature.friends.domain.Friend
import org.andrew.enzona.feature.friends.domain.FriendsRepository
import org.andrew.enzona.feature.friends.presentation.FriendsUiState
import org.andrew.enzona.feature.friends.presentation.FriendsViewModel
import org.andrew.enzona.testing.FakeAccounts
import org.andrew.enzona.testing.sessionOf
import org.andrew.enzona.testing.testAccount
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

private class FakeFriends(private val answer: suspend (String) -> EzResult<List<Friend>>) : FriendsRepository {
    val asked = mutableListOf<String>()
    override suspend fun list(accountUuid: String): EzResult<List<Friend>> {
        asked += accountUuid
        return answer(accountUuid)
    }
}

private fun friend(username: String) = Friend("uuid-$username", username, username.replaceFirstChar { it.uppercase() }, null, null, null)

@OptIn(ExperimentalCoroutinesApi::class)
class FriendsViewModelTest {
    @BeforeTest fun setUp() = Dispatchers.setMain(UnconfinedTestDispatcher())
    @AfterTest fun tearDown() = Dispatchers.resetMain()

    private fun vm(friends: FriendsRepository, accounts: FakeAccounts = FakeAccounts { EzResult.Success(testAccount(uuid = "mine", username = "me")) }) =
        FriendsViewModel(GetAccountUseCase(accounts, sessionOf("me")), friends)

    @Test
    fun itAsksForTheFriendsOfTheUsersOwnAccount() = runTest {
        val repo = FakeFriends { EzResult.Success(listOf(friend("alice"), friend("bob"))) }
        val accounts = FakeAccounts { EzResult.Success(testAccount(uuid = "mine", username = "me")) }
        val v = vm(repo, accounts)
        assertIs<FriendsUiState.Loading>(v.state.value)
        v.load()
        assertEquals(listOf("mine"), repo.asked)
        assertEquals(listOf("me"), accounts.asked)
        assertEquals(listOf("alice", "bob"), (v.state.value as FriendsUiState.Content).friends.map { it.username })
    }

    @Test
    fun noFriendsIsAnEmptyStateNotAnError() = runTest {
        val v = vm(FakeFriends { EzResult.Success(emptyList()) })
        v.load()
        assertIs<FriendsUiState.Empty>(v.state.value)
    }

    @Test
    fun aFailureGettingTheAccountOrTheFriendsIsAnError() = runTest {
        val noAccount = vm(FakeFriends { error("no debe llamarse") }, FakeAccounts { EzResult.Error(Failure.Network("x")) })
        noAccount.load()
        assertIs<FriendsUiState.Error>(noAccount.state.value)

        val noFriends = vm(FakeFriends { EzResult.Error(Failure.Http(500)) })
        noFriends.load()
        assertIs<FriendsUiState.Error>(noFriends.state.value)
    }

    @Test
    fun reloadingKeepsTheListVisibleUntilTheNewOneArrives() = runTest {
        val gate = CompletableDeferred<Unit>()
        var first = true
        val v = vm(FakeFriends {
            if (first) { first = false; EzResult.Success(listOf(friend("alice"))) }
            else { gate.await(); EzResult.Success(listOf(friend("alice"), friend("bob"))) }
        })
        v.load()
        v.load()
        assertEquals(1, (v.state.value as FriendsUiState.Content).friends.size)
        gate.complete(Unit)
        assertEquals(2, (v.state.value as FriendsUiState.Content).friends.size)
    }

    @Test
    fun noFriendNameReachesTheLogs() = runTest {
        withLogging(minimumLevel = LogLevel.TRACE) { sink ->
            val v = vm(FakeFriends { EzResult.Success(listOf(friend("usuario-secreto"))) })
            v.load()
            val written = sink.everythingWritten()
            assertTrue("Amigos cargados" in written, "el test debe observar logs reales")
            for (secret in listOf("usuario-secreto", "Usuario-secreto", "uuid-usuario-secreto", "mine")) {
                assertFalse(secret in written, "$secret apareció en los logs")
            }
        }
    }
}

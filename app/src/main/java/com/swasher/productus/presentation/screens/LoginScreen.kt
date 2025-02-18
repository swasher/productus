package com.swasher.productus.presentation.screens

import android.util.Log
import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetCredentialResponse
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.swasher.productus.R
import com.swasher.productus.presentation.viewmodel.AuthViewModel
import kotlinx.coroutines.launch
import org.json.JSONObject
import androidx.credentials.CustomCredential

@Composable
fun LoginScreen(navController: NavController, viewModel: AuthViewModel = viewModel()) {

    val context = LocalContext.current
    val currentUser by viewModel.currentUser.collectAsState()


    val scope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }


    // val launcher = rememberLauncherForActivityResult(
    //     contract = ActivityResultContracts.StartActivityForResult()
    // ) { result ->
    //     val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
    //     try {
    //         val account = task.getResult(ApiException::class.java)
    //         val idToken = account.idToken
    //         if (idToken != null) {
    //             Log.d("LoginScreen", "idToken: $idToken, emai: ${account.email}")
    //             viewModel.signInWithGoogle(idToken)
    //         }
    //     } catch (e: ApiException) {
    //         Toast.makeText(context, "Ошибка входа: ${e.message}", Toast.LENGTH_SHORT).show()
    //     }
    // }



    LaunchedEffect(currentUser) {
        if (currentUser != null) {
            navController.navigate("folders") {
                popUpTo("loginScreen") { inclusive = true }
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = Color(0xFFeee8dc), // Устанавливаем синий фон
        content = { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(16.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "PRODUCTUS",
                    modifier = Modifier.padding(bottom = 1.dp),
                    style = TextStyle(
                        fontFamily = FontFamily(Font(R.font.darumadrop_one_regular)),
                        fontSize = 46.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF495D92)
                    )
                )
                // Черта
                HorizontalDivider(
                    color = Color(0xFF495D92), // Цвет черты
                    thickness = 4.dp,  // Толщина черты
                    modifier = Modifier.fillMaxWidth().padding(bottom = 60.dp), // Черта на всю ширину
                )
                Image(
                    painter = painterResource(id = R.drawable.login_image),
                    contentDescription = "Login Illustration",
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(250.dp)
                        .clip(RoundedCornerShape(16.dp))
                )
                Spacer(modifier = Modifier.height(32.dp))
                Button(
                    // onClick = {
                    //     val signInIntent = GoogleSignIn.getClient(
                    //         context,
                    //         GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    //             .requestIdToken(context.getString(R.string.default_web_client_id))
                    //             .requestEmail()
                    //             .build()
                    //     ).signInIntent
                    //     launcher.launch(signInIntent)
                    // },

                    onClick = {
                        scope.launch {
                            try {
                                val request = GetCredentialRequest(
                                    listOf(
                                        GetGoogleIdOption.Builder()
                                            .setServerClientId(context.getString(R.string.default_web_client_id))
                                            .build()
                                    )
                                )

                                try {
                                    val result = credentialManager.getCredential(
                                        context,
                                        request
                                    )
                                    Log.d("LoginScreen", "Got credential result: $result")
                                    handleSignInResult(result, viewModel)
                                } catch (e: GetCredentialException) {
                                    Log.e("LoginScreen", "GetCredentialException", e)
                                    Toast.makeText(
                                        context,
                                        "Credential Error: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } catch (e: Exception) {
                                    Log.e("LoginScreen", "Unexpected error", e)
                                    Toast.makeText(
                                        context,
                                        "Unexpected error: ${e.message}",
                                        Toast.LENGTH_LONG
                                    ).show()
                                }



                            } catch (e: GetCredentialException) {
                                Toast.makeText(
                                    context,
                                    "Ошибка входа: ${e.message}",
                                    Toast.LENGTH_SHORT
                                ).show()
                                Log.e("LoginScreen", "Error getting credential", e)
                            }
                        }
                    },


                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Login with Google")
                }
            }
        }
    )
}

// private suspend fun handleSignInResult(
//     result: GetCredentialResponse,
//     viewModel: AuthViewModel
// ) {
//     val credential = result.credential
//     if (credential is GoogleIdTokenCredential) {
//         val idToken = credential.idToken
//         Log.d("LoginScreen", "idToken: $idToken, email: ${credential.id}")
//         viewModel.signInWithGoogle(idToken)
//     }
// }


// private suspend fun handleSignInResult(
//     result: GetCredentialResponse,
//     viewModel: AuthViewModel
// ) {
//     Log.d("LoginScreen", "Starting handleSignInResult")
//     val credential = result.credential
//
//     when (credential) {
//         is GoogleIdTokenCredential -> {
//             Log.d("LoginScreen", "Got GoogleIdTokenCredential")
//             viewModel.signInWithGoogle(credential.idToken)
//         }
//         is CustomCredential -> {
//             // В новом API CustomCredential используется только для паролей,
//             // поэтому этот случай можно пропустить
//             Log.d("LoginScreen", "Unexpected CustomCredential")
//         }
//         else -> {
//             Log.e("LoginScreen", "Unexpected credential type: ${credential?.javaClass?.simpleName}")
//         }
//     }
// }

private suspend fun handleSignInResult(
    result: GetCredentialResponse,
    viewModel: AuthViewModel
) {
    Log.d("LoginScreen", "Starting handleSignInResult")
    val credential = result.credential
    Log.d("LoginScreen", "Credential type: ${credential?.javaClass?.simpleName}")

    when (credential) {
        is GoogleIdTokenCredential -> {
            Log.d("LoginScreen", "Got GoogleIdTokenCredential")
            val token = credential.idToken
            Log.d("LoginScreen", "Token received: ${token.take(10)}...")
            viewModel.signInWithGoogle(token)
        }
        is CustomCredential -> {
            Log.d("LoginScreen", "Got CustomCredential")
            try {
                // Попробуем оба возможных ключа
                var idToken = credential.data.getString("com.google.android.libraries.identity.googleid.BUNDLE_KEY_ID_TOKEN")
                if (idToken == null) {
                    idToken = credential.data.getString("idToken")
                }

                if (idToken != null) {
                    Log.d("LoginScreen", "Token received: ${idToken.take(10)}...")
                    viewModel.signInWithGoogle(idToken)
                } else {
                    // Выведем все доступные ключи в бандле
                    val keys = credential.data.keySet()
                    Log.e("LoginScreen", "Available keys in CustomCredential: $keys")
                    throw Exception("ID token not found in credential data")
                }
            } catch (e: Exception) {
                Log.e("LoginScreen", "Error processing CustomCredential", e)
                throw e
            }
        }
        else -> {
            val error = "Unexpected credential type: ${credential?.javaClass?.simpleName}"
            Log.e("LoginScreen", error)
            throw Exception(error)
        }
    }
}
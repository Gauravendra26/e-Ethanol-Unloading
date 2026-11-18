package com.visibleinfotech.e_ethanolUnloading

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.util.Log
import android.view.KeyEvent
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.visibleinfotech.e_ethanolUnloading.dispatch.DispatchForm
import com.visibleinfotech.e_ethanolUnloading.dispatch.DispatchRecordsScreen
import com.visibleinfotech.e_ethanolUnloading.gateEntry.GateEntry
import com.visibleinfotech.e_ethanolUnloading.gateEntryRecord.GateEntryRecordScreen
import com.visibleinfotech.e_ethanolUnloading.gateEntryNew.GateEntryNew
import com.visibleinfotech.e_ethanolUnloading.indentAndToken.IndentAndTokenScreen
import com.visibleinfotech.e_ethanolUnloading.login.DeviceRegistrationScreen
import com.visibleinfotech.e_ethanolUnloading.login.LoginScreen
import com.visibleinfotech.e_ethanolUnloading.sauda.SaudaForm
import com.visibleinfotech.e_ethanolUnloading.sauda.SaudaRecordsScreen
import com.visibleinfotech.e_ethanolUnloading.sauda.SaudaVerify
import com.visibleinfotech.e_ethanolUnloading.sauda.VerifiedSaudaRecordsScreen
import com.visibleinfotech.e_ethanolUnloading.splash.SplashScreen
import com.visibleinfotech.e_ethanolUnloading.unloading.UnloadingForm
import com.visibleinfotech.e_ethanolUnloading.ui.theme.EEthanolTheme
import com.visibleinfotech.e_ethanolUnloading.unloadRecord.UnloadingRecordForm

class MainActivity : ComponentActivity() {
    private val TAG = "MainActivity"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        WindowCompat.setDecorFitsSystemWindows(window, true)

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED
        ) {

            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.READ_PHONE_STATE,
                    Manifest.permission.RECORD_AUDIO
                ),
                1
            )
        }

        setContent {
            EEthanolTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AppNavigation()
                }
            }
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d(TAG, "onKeyDown - keyCode: $keyCode")

        if (keyCode == 139 || keyCode == 280 || keyCode == 293) {
            Log.d(TAG, "✅ Physical trigger button pressed: $keyCode")

            try {
                val viewModel = ViewModelProvider(this).get(RFIDScannerViewModel::class.java)
                viewModel.scanSingleTag()
                Toast.makeText(this, "Scanning...", Toast.LENGTH_SHORT).show()
            } catch (e: Exception) {
                Log.e(TAG, "Error triggering scan", e)
                Toast.makeText(this, "Error: ${e.message}", Toast.LENGTH_SHORT).show()
            }

            return true
        }

        return super.onKeyDown(keyCode, event)
    }

    override fun onKeyUp(keyCode: Int, event: KeyEvent?): Boolean {
        Log.d(TAG, "onKeyUp - keyCode: $keyCode")
        return super.onKeyUp(keyCode, event)
    }
}

@Composable
fun AppNavigation() {

    val context = androidx.compose.ui.platform.LocalContext.current
    val mainConstant = remember { MainConstant(context) }

    var currentScreen by remember {
        mutableStateOf<AppScreen>(AppScreen.Splash)
    }

    var backPressedOnce by remember {
        mutableStateOf(false)
    }

    LaunchedEffect(backPressedOnce) {

        if (backPressedOnce) {

            kotlinx.coroutines.delay(2000)

            backPressedOnce = false
        }
    }

    LaunchedEffect(Unit) {

        if (mainConstant.isLoggedIn()) {
            currentScreen = AppScreen.Dashboard
        } else {
            currentScreen = AppScreen.Splash
        }
    }

    BackHandler(enabled = currentScreen != AppScreen.Splash) {

        when (currentScreen) {

            AppScreen.GateEntry -> {

                currentScreen = AppScreen.Dashboard
            }

            AppScreen.GateEntryNew -> {

                currentScreen = AppScreen.Dashboard
            }

            AppScreen.GateEntryRecordScreen -> {

                currentScreen = AppScreen.Dashboard
            }
    AppScreen.SaudaForm -> {

                currentScreen = AppScreen.Dashboard
            }

     AppScreen.SaudaVerify -> {

                currentScreen = AppScreen.Dashboard
            }
    AppScreen.VerifiedSaudaRecordsScreen  -> {

                currentScreen = AppScreen.Dashboard
            }
 AppScreen.SaudaRecordsScreen -> {

                currentScreen = AppScreen.Dashboard
            }

            AppScreen.UnloadingForm -> {

                currentScreen = AppScreen.UnloadingForm
            }


            AppScreen.TestingScreen -> {

                currentScreen = AppScreen.Dashboard
            }

            AppScreen.IndentAndTokenScreen -> {

                currentScreen = AppScreen.Dashboard
            }

            AppScreen.DispatchRecordsScreen -> {

                currentScreen = AppScreen.Dashboard
            }

            AppScreen.UnloadingRecordForm -> {

                currentScreen = AppScreen.Dashboard
            }

            AppScreen.DispatchForm -> {

                currentScreen = AppScreen.Dashboard
            }

            AppScreen.Register -> {

                currentScreen = AppScreen.Login
            }

            AppScreen.Login -> {

                currentScreen = AppScreen.Splash
            }

            AppScreen.Dashboard -> {

                if (backPressedOnce) {

                    (context as? android.app.Activity)?.finish()

                } else {

                    backPressedOnce = true

                    android.widget.Toast
                        .makeText(
                            context,
                            "Press again to Exit",
                            android.widget.Toast.LENGTH_SHORT
                        )
                        .show()
                }
            }

            else -> {}
        }

    }

    when (currentScreen) {
        AppScreen.GateEntry -> {
            GateEntry(
                onBack = {
                    currentScreen = AppScreen.Dashboard
                }
            )
        }

        AppScreen.GateEntryNew -> {
            GateEntryNew(
                onBack = {
                    currentScreen = AppScreen.Dashboard
                }
            )
        }

        AppScreen.GateEntryRecordScreen -> {
            GateEntryRecordScreen(
                onBack = {
                    currentScreen = AppScreen.Dashboard
                }
            )
        }
  AppScreen.SaudaForm -> {
      SaudaForm(
                onBack = {
                    currentScreen = AppScreen.Dashboard
                }
            )
        }
 AppScreen.SaudaVerify -> {
     SaudaVerify(
                onBack = {
                    currentScreen = AppScreen.Dashboard
                }
            )
        }
AppScreen.VerifiedSaudaRecordsScreen  -> {
    VerifiedSaudaRecordsScreen (
                onBack = {
                    currentScreen = AppScreen.Dashboard
                }
            )
        }
 AppScreen.SaudaRecordsScreen -> {
     SaudaRecordsScreen(
                onBack = {
                    currentScreen = AppScreen.Dashboard
                }
            )
        }

        AppScreen.UnloadingForm -> {
            UnloadingForm(
                onBack = {
                    currentScreen = AppScreen.Dashboard
                }
            )
        }

        AppScreen.TestingScreen -> {
            TestingScreen(
                onBack = {
                    currentScreen = AppScreen.Dashboard
                }
            )
        }

        AppScreen.IndentAndTokenScreen -> {
            IndentAndTokenScreen(
                onBack = {
                    currentScreen = AppScreen.Dashboard
                }
            )
        }

        AppScreen.DispatchRecordsScreen -> {
            DispatchRecordsScreen(
                onBack = {
                    currentScreen = AppScreen.Dashboard
                }
            )
        }

        AppScreen.UnloadingRecordForm -> {
            UnloadingRecordForm(
                onBack = {
                    currentScreen = AppScreen.Dashboard
                }
            )
        }

        AppScreen.DispatchForm -> {
            DispatchForm(
                onBack = {
                    currentScreen = AppScreen.Dashboard
                }
            )
        }

        AppScreen.Splash -> {
            SplashScreen(
                onNavigateToLogin = {
                    currentScreen = AppScreen.Login
                },
                onNavigateToDashboard = {
                    currentScreen = AppScreen.Dashboard
                }
            )
        }

        AppScreen.Login -> {
            LoginScreen(

                onNavigateToRegister = {
                    currentScreen = AppScreen.Register
                },
                onNavigateToDashboard = {
                    currentScreen = AppScreen.Dashboard
                }
            )
        }

        AppScreen.Register -> {
            DeviceRegistrationScreen(
                onRegistrationComplete = {
                    currentScreen = AppScreen.Login
                },
                onBack = {
                    currentScreen = AppScreen.Login
                }
            )
        }

        AppScreen.Dashboard -> {
            Dashboard(
                onNavigateToGateEntry = {
                    currentScreen = AppScreen.GateEntry
                },
                onNavigateToGateEntryNew = {
                    currentScreen = AppScreen.GateEntryNew
                },
                onNavigateToGateEntryRecordScreen = {
                    currentScreen = AppScreen.GateEntryRecordScreen
                },
                onNavigateToSaudaForm = {
                    currentScreen = AppScreen.SaudaForm
                },
                onNavigateToSaudaVerify = {
                    currentScreen = AppScreen.SaudaVerify
                },
                onNavigateToVerifiedSaudaRecordsScreen = {
                    currentScreen = AppScreen.VerifiedSaudaRecordsScreen
                },
                onNavigateToSaudaRecordsScreen = {
                    currentScreen = AppScreen.SaudaRecordsScreen
                },
                onNavigateToUnloading = {
                    currentScreen = AppScreen.UnloadingForm
                },

                onNavigateToTestingScreen = {
                    currentScreen = AppScreen.TestingScreen
                },
                onNavigateToIndentAndTokenScreen = {
                    currentScreen = AppScreen.IndentAndTokenScreen
                },
  onNavigateToDispatchRecordsScreen = {
                    currentScreen = AppScreen.DispatchRecordsScreen
                },

                onNavigateToUnloadingRecordForm = {
                    currentScreen = AppScreen.UnloadingRecordForm
                },
                onNavigateToDispatchForm = {
                    currentScreen = AppScreen.DispatchForm
                },

                onLogout = {
                    mainConstant.setLoggedIn(false)
                    mainConstant.logout()
                    currentScreen = AppScreen.Login
                }

            )
        }
    }
}

sealed class AppScreen {
    object Splash : AppScreen()
    object Login : AppScreen()
    object Register : AppScreen()
    object GateEntry : AppScreen()
    object GateEntryNew : AppScreen()
    object GateEntryRecordScreen : AppScreen()
    object SaudaForm : AppScreen()
    object SaudaVerify : AppScreen()
    object VerifiedSaudaRecordsScreen  : AppScreen()
    object SaudaRecordsScreen : AppScreen()
    object TestingScreen : AppScreen()
    object UnloadingForm : AppScreen()
    object IndentAndTokenScreen : AppScreen()
    object DispatchRecordsScreen : AppScreen()
    object UnloadingRecordForm : AppScreen()
    object DispatchForm : AppScreen()
    object Dashboard : AppScreen()
}
package dev.celiobenj.seldonfleet

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import dev.celiobenj.seldonfleet.ui.navigation.AppNavHost
import dev.celiobenj.seldonfleet.ui.theme.SeldonFleetTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Tema sempre claro: barras do sistema transparentes com ícones escuros,
        // mesmo com o celular em modo escuro.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        val container = (application as SeldonFleetApp).container
        setContent {
            SeldonFleetTheme {
                AppNavHost(container)
            }
        }
    }
}

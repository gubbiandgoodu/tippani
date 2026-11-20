package com.example.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.DangerRed
import com.example.ui.theme.TippaniTheme

@Composable
fun SetLockPinDialog(
    title: String = "Set Lock PIN",
    subtitle: String = "Create a 4–8 digit PIN to lock and protect your notes and books.",
    onPinSet: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = TippaniTheme.colors
    var pin by remember { mutableStateOf("") }
    var confirmPin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    val inputColors = TextFieldDefaults.colors(
        focusedContainerColor = colors.surfaceVariant,
        unfocusedContainerColor = colors.surfaceVariant,
        focusedTextColor = colors.textPrimary,
        unfocusedTextColor = colors.textPrimary,
        cursorColor = colors.accent,
        focusedIndicatorColor = colors.accent,
        unfocusedIndicatorColor = colors.border
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("set_lock_pin_dialog"),
        containerColor = colors.surface,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Set Lock PIN",
                tint = colors.accent,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() } && value.length <= 8) {
                            pin = value
                            errorMessage = null
                        }
                    },
                    placeholder = { Text("Enter PIN (4–8 digits)", color = colors.textMuted) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(14.dp),
                    colors = inputColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("set_pin_input")
                )
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedTextField(
                    value = confirmPin,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() } && value.length <= 8) {
                            confirmPin = value
                            errorMessage = null
                        }
                    },
                    placeholder = { Text("Confirm PIN", color = colors.textMuted) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(14.dp),
                    colors = inputColors,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("confirm_pin_input")
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        fontSize = 12.sp,
                        color = DangerRed
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    when {
                        pin.length < 4 -> errorMessage = "PIN must be at least 4 digits."
                        pin != confirmPin -> errorMessage = "PINs do not match."
                        else -> onPinSet(pin)
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.onAccent
                ),
                modifier = Modifier.testTag("set_pin_confirm_btn")
            ) {
                Text("Set PIN & Lock")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

@Composable
fun VerifyLockPinDialog(
    title: String,
    subtitle: String = "Enter your PIN to continue.",
    confirmLabel: String = "Unlock",
    onVerify: (String) -> Boolean,
    onSuccess: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = TippaniTheme.colors
    var pin by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        modifier = Modifier.testTag("verify_lock_pin_dialog"),
        containerColor = colors.surface,
        shape = RoundedCornerShape(24.dp),
        icon = {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = "Locked",
                tint = colors.accent,
                modifier = Modifier.size(28.dp)
            )
        },
        title = {
            Text(
                text = title,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )
        },
        text = {
            Column {
                Text(
                    text = subtitle,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
                Spacer(modifier = Modifier.height(14.dp))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { value ->
                        if (value.all { it.isDigit() } && value.length <= 8) {
                            pin = value
                            errorMessage = null
                        }
                    },
                    placeholder = { Text("Enter PIN", color = colors.textMuted) },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(14.dp),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = colors.surfaceVariant,
                        unfocusedContainerColor = colors.surfaceVariant,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        cursorColor = colors.accent,
                        focusedIndicatorColor = colors.accent,
                        unfocusedIndicatorColor = colors.border
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("verify_pin_input")
                )
                if (errorMessage != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = errorMessage!!,
                        fontSize = 12.sp,
                        color = DangerRed
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (onVerify(pin)) {
                        onSuccess()
                    } else {
                        errorMessage = "Incorrect PIN. Try again."
                        pin = ""
                    }
                },
                enabled = pin.length >= 4,
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.onAccent
                ),
                modifier = Modifier.testTag("verify_pin_confirm_btn")
            ) {
                Text(confirmLabel)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

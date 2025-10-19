package com.runanywhere.startup_hackathon20

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.runanywhere.startup_hackathon20.ui.theme.Startup_hackathon20Theme
import com.runanywhere.sdk.public.RunAnywhere
import com.runanywhere.sdk.public.extensions.listAvailableModels
import com.runanywhere.sdk.models.ModelInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Startup_hackathon20Theme {
                MainApp()
            }
        }
    }
}

// More soothing color palette
val SoftLavender = Color(0xFFF8F6FF)
val PaleLilac = Color(0xFFE8E4F3)
val GentleViolet = Color(0xFFB39DDB)
val MutedPlum = Color(0xFF8E6AA8)
val WarmCream = Color(0xFFFFFDF9)
val SoftMint = Color(0xFFE8F5E8)

// Dark mode colors
val DarkBackground = Color(0xFF1A1A1A)
val DarkSurface = Color(0xFF2D2D30)
val DarkAccent = Color(0xFF9C88B8)
val DarkText = Color(0xFFE8E8E8)

// Form Data Classes
data class HealthProfile(
    val name: String = "",
    val age: String = "",
    val primaryConcern: String = "",
    val activityLevel: String = "",
    val dietaryRestrictions: String = "",
    val currentSymptoms: List<String> = emptyList(),
    val healthGoals: String = "",
    val timeAvailable: String = "",
    val hasPCOS: String = "",
    val hasThyroidIssues: String = "",
    val menopauseStatus: String = ""
)

data class MentalHealthProfile(
    val stressLevel: Int = 0, // 1-10 scale
    val sleepQuality: Int = 0, // 1-10 scale
    val energyLevel: Int = 0, // 1-10 scale
    val moodStability: Int = 0, // 1-10 scale
    val anxietyFrequency: Int = 0, // 1-10 scale
    val socialConnection: Int = 0, // 1-10 scale
    val workLifeBalance: Int = 0, // 1-10 scale
    val selfEsteem: Int = 0, // 1-10 scale
    val copingAbility: Int = 0, // 1-10 scale
    val overallSatisfaction: Int = 0, // 1-10 scale
    val concentrationLevel: Int = 0, // 1-10 scale
    val emotionalStability: Int = 0, // 1-10 scale
    val additionalComments: String = ""
)

data class ChatMessage(val text: String, val isUser: Boolean)

// Navigation destinations
enum class Screen {
    HealthAssessment,
    Chat,
    MentalHealthTracker
}

// Enhanced ViewModel with navigation and theme support
class EnhancedChatViewModel : ViewModel() {

    // Navigation state
    private val _currentScreen = MutableStateFlow(Screen.HealthAssessment)
    val currentScreen: StateFlow<Screen> = _currentScreen

    // Theme state
    private val _isDarkMode = MutableStateFlow(false)
    val isDarkMode: StateFlow<Boolean> = _isDarkMode

    // Chat functionality
    private val _messages = MutableStateFlow<List<ChatMessage>>(emptyList())
    val messages: StateFlow<List<ChatMessage>> = _messages

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading

    private val _downloadProgress = MutableStateFlow<Float?>(null)
    val downloadProgress: StateFlow<Float?> = _downloadProgress

    private val _currentModelId = MutableStateFlow<String?>(null)
    val currentModelId: StateFlow<String?> = _currentModelId

    private val _statusMessage = MutableStateFlow<String>("Initializing...")
    val statusMessage: StateFlow<String> = _statusMessage

    // Default model to use
    private val defaultModelId = "qwen2.5-0.5b-instruct-q6_k"

    // Form-specific state
    private val _userProfile = mutableStateOf(HealthProfile())
    val userProfile: State<HealthProfile> = _userProfile

    private val _mentalHealthProfile = mutableStateOf(MentalHealthProfile())
    val mentalHealthProfile: State<MentalHealthProfile> = _mentalHealthProfile

    init {
        initializeDefaultModel()
    }

    private fun initializeDefaultModel() {
        viewModelScope.launch {
            try {
                _statusMessage.value = "Setting up AI model..."

                // Get available models
                val models = listAvailableModels()

                // Debug: Log all available models
                models.forEach { model ->
                    println("Available model: ID='${model.id}', Name='${model.name}', Downloaded=${model.isDownloaded}")
                }

                // Try different possible IDs for the Qwen model
                val possibleIds = listOf(
                    "qwen2.5-0.5b-instruct-q6_k",
                    "qwen2.5-0.5b-instruct-q6_k.gguf",
                    "Qwen 2.5 0.5B Instruct Q6_K",
                    "qwen2.5-0.5b-instruct-q6_k.gguf"
                )

                val targetModel = models.find { model ->
                    possibleIds.any { id -> model.id.equals(id, ignoreCase = true) }
                } ?: models.find { model ->
                    model.name.contains("Qwen", ignoreCase = true)
                }

                if (targetModel == null) {
                    _statusMessage.value =
                        "Qwen model not found. Available models: ${models.map { "${it.name} (${it.id})" }}"
                    return@launch
                }

                println("Selected model: ID='${targetModel.id}', Name='${targetModel.name}'")

                // Check if model is already downloaded
                if (!targetModel.isDownloaded) {
                    // Download the model
                    _statusMessage.value = "Downloading AI model (374 MB)..."
                    RunAnywhere.downloadModel(targetModel.id).collect { progress ->
                        _downloadProgress.value = progress
                        _statusMessage.value = "Downloading: ${(progress * 100).toInt()}%"
                    }
                    _downloadProgress.value = null
                }

                // Load the model
                _statusMessage.value = "Loading AI model..."
                val success = RunAnywhere.loadModel(targetModel.id)
                if (success) {
                    _currentModelId.value = targetModel.id
                    _statusMessage.value = "Ready to chat!"
                } else {
                    _statusMessage.value = "Failed to load AI model"
                }

            } catch (e: Exception) {
                _statusMessage.value = "Error setting up AI: ${e.message}"
                _downloadProgress.value = null
            }
        }
    }

    fun navigateTo(screen: Screen) {
        _currentScreen.value = screen
    }

    fun toggleTheme() {
        _isDarkMode.value = !_isDarkMode.value
    }

    fun sendMessage(text: String) {
        if (_currentModelId.value == null) {
            _statusMessage.value = "AI model not ready"
            return
        }

        // Add user message
        _messages.value += ChatMessage(text, isUser = true)

        viewModelScope.launch {
            _isLoading.value = true

            try {
                // Generate response with streaming
                var assistantResponse = ""
                RunAnywhere.generateStream(text).collect { token ->
                    assistantResponse += token

                    // Update assistant message in real-time
                    val currentMessages = _messages.value.toMutableList()
                    if (currentMessages.lastOrNull()?.isUser == false) {
                        currentMessages[currentMessages.lastIndex] =
                            ChatMessage(assistantResponse, isUser = false)
                    } else {
                        currentMessages.add(ChatMessage(assistantResponse, isUser = false))
                    }
                    _messages.value = currentMessages
                }
            } catch (e: Exception) {
                _messages.value += ChatMessage("Error: ${e.message}", isUser = false)
            }

            _isLoading.value = false
        }
    }

    fun updateProfile(profile: HealthProfile) {
        _userProfile.value = profile
    }

    fun updateMentalHealthProfile(profile: MentalHealthProfile) {
        _mentalHealthProfile.value = profile
    }

    fun generatePromptFromProfile(profile: HealthProfile): String {
        return buildString {
            append("I'm a ${profile.age}-year-old woman named ${profile.name}. ")
            append("My main health concern is: ${profile.primaryConcern}. ")
            append("My activity level is ${profile.activityLevel}. ")

            if (profile.dietaryRestrictions.isNotBlank()) {
                append("I have these dietary restrictions: ${profile.dietaryRestrictions}. ")
            }

            if (profile.currentSymptoms.isNotEmpty()) {
                append("I'm currently experiencing: ${profile.currentSymptoms.joinToString(", ")}. ")
            }

            append("My health goals are: ${profile.healthGoals}. ")
            append("I have ${profile.timeAvailable} available for meal prep and exercise. ")
            append("Please provide personalized nutrition advice and recommendations specific to my situation as a woman over 35.")
            if (profile.hasPCOS.isNotBlank()) {
                append(" I have ${profile.hasPCOS}.")
            }
            if (profile.hasThyroidIssues.isNotBlank()) {
                append(" I have ${profile.hasThyroidIssues}.")
            }
            if (profile.menopauseStatus.isNotBlank()) {
                append(" I am ${profile.menopauseStatus}.")
            }
        }
    }

    fun calculateMentalHealthScore(profile: MentalHealthProfile): Int {
        // Calculate weighted average of all factors
        // Lower anxiety frequency is better, so we invert it
        val invertedAnxiety = 11 - profile.anxietyFrequency

        val totalScore = profile.stressLevel + profile.sleepQuality + profile.energyLevel +
                profile.moodStability + invertedAnxiety + profile.socialConnection +
                profile.workLifeBalance + profile.selfEsteem + profile.copingAbility +
                profile.overallSatisfaction + profile.concentrationLevel + profile.emotionalStability

        // Convert to percentage (max possible score is 120)
        return ((totalScore / 120.0) * 100).toInt()
    }

    fun getMentalHealthCategory(score: Int): String {
        return when (score) {
            in 85..100 -> "Excellent Mental Health"
            in 70..84 -> "Good Mental Health"
            in 55..69 -> "Fair Mental Health"
            in 40..54 -> "Poor Mental Health"
            else -> "Seek Professional Support"
        }
    }
}

@Composable
fun MainApp(viewModel: EnhancedChatViewModel = viewModel()) {
    val currentScreen by viewModel.currentScreen.collectAsState()
    val isDarkMode by viewModel.isDarkMode.collectAsState()
    
    // Check if using gesture navigation
    val context = LocalContext.current
    val activity = context as? ComponentActivity
    var isGestureNavigation by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        activity?.let {
            val rootView = it.findViewById<android.view.View>(android.R.id.content)
            val insets = ViewCompat.getRootWindowInsets(rootView)
            val navBarHeight =
                insets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
            isGestureNavigation = navBarHeight < 100
        }
    }

    val backgroundColor = if (isDarkMode) DarkBackground else SoftLavender
    val surfaceColor = if (isDarkMode) DarkSurface else WarmCream

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = if (isDarkMode) {
                        listOf(DarkBackground, DarkSurface)
                    } else {
                        listOf(SoftLavender, WarmCream)
                    }
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Content based on current screen
            Box(
                modifier = Modifier.weight(1f)
            ) {
                when (currentScreen) {
                    Screen.HealthAssessment -> HealthAssessmentScreen(
                        viewModel = viewModel,
                        isDarkMode = isDarkMode
                    )
                    Screen.Chat -> ChatScreen(
                        viewModel = viewModel,
                        isDarkMode = isDarkMode
                    )
                    Screen.MentalHealthTracker -> MentalHealthTrackerScreen(
                        viewModel = viewModel,
                        isDarkMode = isDarkMode
                    )
                }
            }

            // Bottom Navigation Bar
            BottomNavigationBar(
                currentScreen = currentScreen,
                isDarkMode = isDarkMode,
                isGestureNavigation = isGestureNavigation,
                onNavigate = { screen -> viewModel.navigateTo(screen) },
                onToggleTheme = { viewModel.toggleTheme() }
            )
        }
    }
}

@Composable
fun WelcomeMessage(
    isDarkMode: Boolean,
    onStartAssessment: () -> Unit
) {
    val textColor = if (isDarkMode) DarkText else Color.Black
    val accentColor = if (isDarkMode) DarkAccent else GentleViolet
    val backgroundColor = if (isDarkMode) DarkAccent.copy(alpha = 0.1f) else GentleViolet.copy(alpha = 0.1f)

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Main welcome message
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp),
            contentAlignment = Alignment.Center
        ) {
            Surface(
                modifier = Modifier
                    .background(
                        color = backgroundColor,
                        shape = RoundedCornerShape(50.dp)
                    )
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                color = Color.Transparent
            ) {
                Text(
                    text = "Hey! I'm here to help you with personalized nutrition advice.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = textColor,
                    fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center
                )
            }
        }

        // Quick assessment button
        Button(
            onClick = onStartAssessment,
            modifier = Modifier.padding(horizontal = 32.dp),
            shape = RoundedCornerShape(25.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = Color.White
            )
        ) {
            Text(
                "🌸 Start Health Assessment",
                fontWeight = FontWeight.Medium,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
    }
}

@Composable
fun HealthAssessmentScreen(
    viewModel: EnhancedChatViewModel,
    isDarkMode: Boolean
) {
    val userProfile by viewModel.userProfile
    var profile by remember { mutableStateOf(userProfile) }

    val backgroundColor = if (isDarkMode) DarkBackground else SoftLavender
    val surfaceColor = if (isDarkMode) DarkSurface else WarmCream
    val textColor = if (isDarkMode) DarkText else Color.Black
    val accentColor = if (isDarkMode) DarkAccent else GentleViolet

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            text = "🌸 Health Assessment",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Help me understand your unique wellness needs",
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDarkMode) DarkText.copy(alpha = 0.8f) else MutedPlum.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Name Field
        OutlinedTextField(
            value = profile.name,
            onValueChange = { profile = profile.copy(name = it) },
            label = { Text("What should I call you?") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                focusedLabelColor = accentColor,
                cursorColor = accentColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            ),
            shape = RoundedCornerShape(16.dp)
        )

        // Age Field
        OutlinedTextField(
            value = profile.age,
            onValueChange = { profile = profile.copy(age = it) },
            label = { Text("Your age") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                focusedLabelColor = accentColor,
                cursorColor = accentColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            ),
            shape = RoundedCornerShape(16.dp)
        )

        // Primary Concern
        Text(
            text = "What's your main health concern?",
            fontWeight = FontWeight.Medium,
            color = textColor,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val concerns = listOf(
            "Weight management",
            "Energy & fatigue",
            "Hormonal changes",
            "Digestive issues",
            "Bone health",
            "Heart health",
            "Skin & aging"
        )

        concerns.forEach { concern ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = profile.primaryConcern == concern,
                        onClick = { profile = profile.copy(primaryConcern = concern) }
                    )
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = profile.primaryConcern == concern,
                    onClick = { profile = profile.copy(primaryConcern = concern) },
                    colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                )
                Text(
                    text = concern,
                    modifier = Modifier.padding(start = 12.dp),
                    color = textColor
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Activity Level
        Text(
            text = "How active are you?",
            fontWeight = FontWeight.Medium,
            color = textColor,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val activityLevels = listOf(
            "Sedentary (desk job, minimal exercise)",
            "Lightly active (light exercise 1-3 days/week)",
            "Moderately active (moderate exercise 3-5 days/week)",
            "Very active (hard exercise 6-7 days/week)"
        )

        activityLevels.forEach { level ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = profile.activityLevel == level,
                        onClick = { profile = profile.copy(activityLevel = level) }
                    )
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = profile.activityLevel == level,
                    onClick = { profile = profile.copy(activityLevel = level) },
                    colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                )
                Text(
                    text = level,
                    modifier = Modifier.padding(start = 12.dp),
                    fontSize = 14.sp,
                    color = textColor
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Dietary Restrictions
        OutlinedTextField(
            value = profile.dietaryRestrictions,
            onValueChange = { profile = profile.copy(dietaryRestrictions = it) },
            label = { Text("Any dietary restrictions or allergies?") },
            placeholder = { Text("vegetarian, gluten-free, etc.") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                focusedLabelColor = accentColor,
                cursorColor = accentColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            ),
            shape = RoundedCornerShape(16.dp),
            minLines = 2
        )

        // Health Goals
        OutlinedTextField(
            value = profile.healthGoals,
            onValueChange = { profile = profile.copy(healthGoals = it) },
            label = { Text("What are your main health goals?") },
            placeholder = { Text("lose weight, increase energy, etc.") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                focusedLabelColor = accentColor,
                cursorColor = accentColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            ),
            shape = RoundedCornerShape(16.dp),
            minLines = 2
        )

        // Time Available
        Text(
            text = "How much time can you dedicate to meal prep weekly?",
            fontWeight = FontWeight.Medium,
            color = textColor,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val timeOptions = listOf(
            "Less than 2 hours",
            "2-4 hours",
            "4-6 hours",
            "More than 6 hours"
        )

        timeOptions.forEach { time ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = profile.timeAvailable == time,
                        onClick = { profile = profile.copy(timeAvailable = time) }
                    )
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = profile.timeAvailable == time,
                    onClick = { profile = profile.copy(timeAvailable = time) },
                    colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                )
                Text(
                    text = time,
                    modifier = Modifier.padding(start = 12.dp),
                    color = textColor
                )
            }
        }

        // PCOS
        Text(
            text = "Do you have PCOS?",
            fontWeight = FontWeight.Medium,
            color = textColor,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val pcosOptions = listOf(
            "Yes",
            "No",
            "Unsure"
        )

        pcosOptions.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = profile.hasPCOS == option,
                        onClick = { profile = profile.copy(hasPCOS = option) }
                    )
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = profile.hasPCOS == option,
                    onClick = { profile = profile.copy(hasPCOS = option) },
                    colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                )
                Text(
                    text = option,
                    modifier = Modifier.padding(start = 12.dp),
                    color = textColor
                )
            }
        }

        // Thyroid Issues
        Text(
            text = "Do you have thyroid issues?",
            fontWeight = FontWeight.Medium,
            color = textColor,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val thyroidOptions = listOf(
            "Yes",
            "No",
            "Unsure"
        )

        thyroidOptions.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = profile.hasThyroidIssues == option,
                        onClick = { profile = profile.copy(hasThyroidIssues = option) }
                    )
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = profile.hasThyroidIssues == option,
                    onClick = { profile = profile.copy(hasThyroidIssues = option) },
                    colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                )
                Text(
                    text = option,
                    modifier = Modifier.padding(start = 12.dp),
                    color = textColor
                )
            }
        }

        // Menopause Status
        Text(
            text = "What is your menopause status?",
            fontWeight = FontWeight.Medium,
            color = textColor,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        val menopauseOptions = listOf(
            "Pre-menopause",
            "Peri-menopause",
            "Post-menopause"
        )

        menopauseOptions.forEach { option ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = profile.menopauseStatus == option,
                        onClick = { profile = profile.copy(menopauseStatus = option) }
                    )
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RadioButton(
                    selected = profile.menopauseStatus == option,
                    onClick = { profile = profile.copy(menopauseStatus = option) },
                    colors = RadioButtonDefaults.colors(selectedColor = accentColor)
                )
                Text(
                    text = option,
                    modifier = Modifier.padding(start = 12.dp),
                    color = textColor
                )
            }
        }

        Spacer(modifier = Modifier.height(40.dp))

        // Submit Button
        Button(
            onClick = {
                viewModel.updateProfile(profile)
                viewModel.navigateTo(Screen.Chat)
                val prompt = viewModel.generatePromptFromProfile(profile)
                viewModel.sendMessage(prompt)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = Color.White
            ),
            enabled = profile.name.isNotBlank() &&
                    profile.age.isNotBlank() &&
                    profile.primaryConcern.isNotBlank()
        ) {
            Text(
                "✨ Get My Personalized Plan",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(100.dp)) // Extra space for bottom nav
    }
}

@Composable
fun MessageBubble(message: ChatMessage, isDarkMode: Boolean) {
    val userBubbleColor = if (isDarkMode) DarkAccent else GentleViolet
    val assistantBubbleColor = if (isDarkMode) DarkSurface else WarmCream.copy(alpha = 0.9f)
    val userTextColor = Color.White
    val assistantTextColor = if (isDarkMode) DarkText else Color.Black.copy(alpha = 0.8f)
    val labelColor = if (isDarkMode) {
        if (message.isUser) Color.White.copy(alpha = 0.8f) else DarkText.copy(alpha = 0.7f)
    } else {
        if (message.isUser) Color.White.copy(alpha = 0.8f) else MutedPlum.copy(alpha = 0.7f)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (message.isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.widthIn(max = 280.dp),
            shape = RoundedCornerShape(
                topStart = 20.dp,
                topEnd = 20.dp,
                bottomStart = if (message.isUser) 20.dp else 4.dp,
                bottomEnd = if (message.isUser) 4.dp else 20.dp
            ),
            colors = CardDefaults.cardColors(
                containerColor = if (message.isUser) userBubbleColor else assistantBubbleColor
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (message.isUser) "You" else "🌸 NutriHer",
                    style = MaterialTheme.typography.labelSmall,
                    color = labelColor,
                    fontWeight = FontWeight.Medium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = message.text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message.isUser) userTextColor else assistantTextColor,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun BottomNavigationBar(
    currentScreen: Screen,
    isDarkMode: Boolean,
    isGestureNavigation: Boolean,
    onNavigate: (Screen) -> Unit,
    onToggleTheme: () -> Unit
) {
    val backgroundColor = if (isDarkMode) DarkSurface else WarmCream
    val contentColor = if (isDarkMode) DarkText else MutedPlum
    val selectedColor = if (isDarkMode) DarkAccent else GentleViolet

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (!isGestureNavigation) {
                    Modifier.navigationBarsPadding()
                } else {
                    Modifier.padding(bottom = 16.dp)
                }
            ),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Health Assessment Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onNavigate(Screen.HealthAssessment) }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.AccountCircle,
                    contentDescription = "Health Assessment",
                    tint = if (currentScreen == Screen.HealthAssessment) selectedColor else contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Assessment",
                    fontSize = 12.sp,
                    color = if (currentScreen == Screen.HealthAssessment) selectedColor else contentColor,
                    fontWeight = if (currentScreen == Screen.HealthAssessment) FontWeight.Medium else FontWeight.Normal
                )
            }

            // Chat Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onNavigate(Screen.Chat) }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Send,
                    contentDescription = "AI Chat",
                    tint = if (currentScreen == Screen.Chat) selectedColor else contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "AI Chat",
                    fontSize = 12.sp,
                    color = if (currentScreen == Screen.Chat) selectedColor else contentColor,
                    fontWeight = if (currentScreen == Screen.Chat) FontWeight.Medium else FontWeight.Normal
                )
            }

            // Mental Health Tracker Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onNavigate(Screen.MentalHealthTracker) }
                    .padding(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Mental Health Tracker",
                    tint = if (currentScreen == Screen.MentalHealthTracker) selectedColor else contentColor,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Mental Health",
                    fontSize = 12.sp,
                    color = if (currentScreen == Screen.MentalHealthTracker) selectedColor else contentColor,
                    fontWeight = if (currentScreen == Screen.MentalHealthTracker) FontWeight.Medium else FontWeight.Normal
                )
            }

            // Theme Toggle Button
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clickable { onToggleTheme() }
                    .padding(8.dp)
            ) {
                // Use text instead of icon for theme toggle
                Box(
                    modifier = Modifier.size(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isDarkMode) "☀️" else "🌙",
                        fontSize = 20.sp,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (isDarkMode) "Light" else "Dark",
                    fontSize = 12.sp,
                    color = contentColor,
                    fontWeight = FontWeight.Normal
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    viewModel: EnhancedChatViewModel,
    isDarkMode: Boolean
) {
    val messages by viewModel.messages.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val downloadProgress by viewModel.downloadProgress.collectAsState()
    val currentModelId by viewModel.currentModelId.collectAsState()
    val statusMessage by viewModel.statusMessage.collectAsState()

    var inputText by remember { mutableStateOf("") }

    val backgroundColor = if (isDarkMode) DarkBackground else SoftLavender
    val surfaceColor = if (isDarkMode) DarkSurface else WarmCream
    val textColor = if (isDarkMode) DarkText else Color.Black
    val accentColor = if (isDarkMode) DarkAccent else GentleViolet

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // Custom App Bar
        Surface(
            modifier = Modifier.fillMaxWidth(),
            color = Color.Transparent
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 16.dp)
                    .statusBarsPadding(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "🌸 NutriHer Chat",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Medium,
                    color = accentColor
                )
            }
        }

        // Status bar with elegant design
        if (downloadProgress != null || statusMessage != "Ready to chat!") {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDarkMode) DarkSurface else PaleLilac.copy(alpha = 0.5f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = statusMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = accentColor,
                        fontWeight = FontWeight.Medium
                    )
                    downloadProgress?.let { progress ->
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progress },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp)),
                            color = accentColor,
                            trackColor = if (isDarkMode) DarkSurface else SoftLavender
                        )
                    }
                }
            }
        }

        // Messages List
        val listState = rememberLazyListState()

        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (messages.isEmpty() && currentModelId != null) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Surface(
                            modifier = Modifier
                                .background(
                                    color = if (isDarkMode) DarkAccent.copy(alpha = 0.1f) else GentleViolet.copy(
                                        alpha = 0.1f
                                    ),
                                    shape = RoundedCornerShape(50.dp)
                                )
                                .padding(horizontal = 24.dp, vertical = 16.dp),
                            color = Color.Transparent
                        ) {
                            Text(
                                text = "Hey! I'm here to help you with personalized nutrition advice. Ask me anything!",
                                style = MaterialTheme.typography.bodyMedium,
                                color = if (isDarkMode) DarkText else Color.Black,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            items(messages) { message ->
                MessageBubble(message, isDarkMode)
            }
        }

        // Auto-scroll to bottom when new messages arrive
        LaunchedEffect(messages.size) {
            if (messages.isNotEmpty()) {
                listState.animateScrollToItem(messages.size - 1)
            }
        }

        // Input Field
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            shape = RoundedCornerShape(25.dp),
            colors = CardDefaults.cardColors(
                containerColor = surfaceColor.copy(alpha = 0.9f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextField(
                    value = inputText,
                    onValueChange = { inputText = it },
                    modifier = Modifier.weight(1f),
                    placeholder = {
                        Text(
                            "Ask about nutrition...",
                            color = if (isDarkMode) DarkText.copy(alpha = 0.6f) else MutedPlum.copy(
                                alpha = 0.6f
                            )
                        )
                    },
                    enabled = !isLoading && currentModelId != null,
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        cursorColor = accentColor,
                        focusedTextColor = textColor,
                        unfocusedTextColor = textColor
                    ),
                    shape = RoundedCornerShape(20.dp)
                )

                FloatingActionButton(
                    onClick = {
                        if (inputText.isNotBlank()) {
                            viewModel.sendMessage(inputText)
                            inputText = ""
                        }
                    },
                    modifier = Modifier.size(48.dp),
                    containerColor = accentColor,
                    contentColor = Color.White,
                    elevation = FloatingActionButtonDefaults.elevation(
                        defaultElevation = 6.dp
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp)) // Reduced space for bottom nav
    }
}

@Composable
fun MentalHealthTrackerScreen(
    viewModel: EnhancedChatViewModel,
    isDarkMode: Boolean
) {
    val mentalHealthProfile by viewModel.mentalHealthProfile
    var profile by remember { mutableStateOf(mentalHealthProfile) }

    val backgroundColor = if (isDarkMode) DarkBackground else SoftLavender
    val surfaceColor = if (isDarkMode) DarkSurface else WarmCream
    val textColor = if (isDarkMode) DarkText else Color.Black
    val accentColor = if (isDarkMode) DarkAccent else GentleViolet

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .statusBarsPadding()
            .verticalScroll(rememberScrollState())
    ) {
        // Header
        Text(
            text = "🌸 Mental Health Tracker",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = accentColor,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Text(
            text = "Help me understand your mental health needs",
            style = MaterialTheme.typography.bodyLarge,
            color = if (isDarkMode) DarkText.copy(alpha = 0.8f) else MutedPlum.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // Stress Level
        Text(
            text = "What is your current stress level? (1-10)",
            fontWeight = FontWeight.Medium,
            color = textColor,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Slider(
            value = profile.stressLevel.toFloat(),
            onValueChange = { profile = profile.copy(stressLevel = it.toInt()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            valueRange = 1f..10f,
            steps = 9
        )
        Text(
            text = "Stress Level: ${profile.stressLevel}",
            color = textColor
        )

        // Sleep Quality
        Text(
            text = "What is your current sleep quality? (1-10)",
            fontWeight = FontWeight.Medium,
            color = textColor,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Slider(
            value = profile.sleepQuality.toFloat(),
            onValueChange = { profile = profile.copy(sleepQuality = it.toInt()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            valueRange = 1f..10f,
            steps = 9
        )
        Text(
            text = "Sleep Quality: ${profile.sleepQuality}",
            color = textColor
        )

        // Energy Level
        Text(
            text = "What is your current energy level? (1-10)",
            fontWeight = FontWeight.Medium,
            color = textColor,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Slider(
            value = profile.energyLevel.toFloat(),
            onValueChange = { profile = profile.copy(energyLevel = it.toInt()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            valueRange = 1f..10f,
            steps = 9
        )
        Text(
            text = "Energy Level: ${profile.energyLevel}",
            color = textColor
        )

        // Mood Stability
        Text(
            text = "What is your current mood stability? (1-10)",
            fontWeight = FontWeight.Medium,
            color = textColor,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Slider(
            value = profile.moodStability.toFloat(),
            onValueChange = { profile = profile.copy(moodStability = it.toInt()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            valueRange = 1f..10f,
            steps = 9
        )
        Text(
            text = "Mood Stability: ${profile.moodStability}",
            color = textColor
        )

        // Anxiety Frequency
        Text(
            text = "How often do you experience anxiety? (1-10)",
            fontWeight = FontWeight.Medium,
            color = textColor,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Slider(
            value = profile.anxietyFrequency.toFloat(),
            onValueChange = { profile = profile.copy(anxietyFrequency = it.toInt()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            valueRange = 1f..10f,
            steps = 9
        )
        Text(
            text = "Anxiety Frequency: ${profile.anxietyFrequency}",
            color = textColor
        )

        // Social Connection
        Text(
            text = "How connected do you feel to others? (1-10)",
            fontWeight = FontWeight.Medium,
            color = textColor,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Slider(
            value = profile.socialConnection.toFloat(),
            onValueChange = { profile = profile.copy(socialConnection = it.toInt()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            valueRange = 1f..10f,
            steps = 9
        )
        Text(
            text = "Social Connection: ${profile.socialConnection}",
            color = textColor
        )

        // Work-Life Balance
        Text(
            text = "How balanced is your work and personal life? (1-10)",
            fontWeight = FontWeight.Medium,
            color = textColor,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Slider(
            value = profile.workLifeBalance.toFloat(),
            onValueChange = { profile = profile.copy(workLifeBalance = it.toInt()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            valueRange = 1f..10f,
            steps = 9
        )
        Text(
            text = "Work-Life Balance: ${profile.workLifeBalance}",
            color = textColor
        )

        // Self Esteem
        Text(
            text = "How confident do you feel in yourself? (1-10)",
            fontWeight = FontWeight.Medium,
            color = textColor,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Slider(
            value = profile.selfEsteem.toFloat(),
            onValueChange = { profile = profile.copy(selfEsteem = it.toInt()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            valueRange = 1f..10f,
            steps = 9
        )
        Text(
            text = "Self Esteem: ${profile.selfEsteem}",
            color = textColor
        )

        // Coping Ability
        Text(
            text = "How well do you cope with stress? (1-10)",
            fontWeight = FontWeight.Medium,
            color = textColor,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Slider(
            value = profile.copingAbility.toFloat(),
            onValueChange = { profile = profile.copy(copingAbility = it.toInt()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            valueRange = 1f..10f,
            steps = 9
        )
        Text(
            text = "Coping Ability: ${profile.copingAbility}",
            color = textColor
        )

        // Overall Satisfaction
        Text(
            text = "How satisfied are you with your life? (1-10)",
            fontWeight = FontWeight.Medium,
            color = textColor,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Slider(
            value = profile.overallSatisfaction.toFloat(),
            onValueChange = { profile = profile.copy(overallSatisfaction = it.toInt()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            valueRange = 1f..10f,
            steps = 9
        )
        Text(
            text = "Overall Satisfaction: ${profile.overallSatisfaction}",
            color = textColor
        )

        // Concentration Level
        Text(
            text = "How focused do you feel? (1-10)",
            fontWeight = FontWeight.Medium,
            color = textColor,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Slider(
            value = profile.concentrationLevel.toFloat(),
            onValueChange = { profile = profile.copy(concentrationLevel = it.toInt()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            valueRange = 1f..10f,
            steps = 9
        )
        Text(
            text = "Concentration Level: ${profile.concentrationLevel}",
            color = textColor
        )

        // Emotional Stability
        Text(
            text = "How emotionally stable do you feel? (1-10)",
            fontWeight = FontWeight.Medium,
            color = textColor,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 12.dp)
        )

        Slider(
            value = profile.emotionalStability.toFloat(),
            onValueChange = { profile = profile.copy(emotionalStability = it.toInt()) },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            valueRange = 1f..10f,
            steps = 9
        )
        Text(
            text = "Emotional Stability: ${profile.emotionalStability}",
            color = textColor
        )

        // Additional Comments
        OutlinedTextField(
            value = profile.additionalComments,
            onValueChange = { profile = profile.copy(additionalComments = it) },
            label = { Text("Any additional comments about your mental health?") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 20.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = accentColor,
                focusedLabelColor = accentColor,
                cursorColor = accentColor,
                focusedTextColor = textColor,
                unfocusedTextColor = textColor
            ),
            shape = RoundedCornerShape(16.dp),
            minLines = 2
        )

        // Mental Health Score Display
        val mentalHealthScore = viewModel.calculateMentalHealthScore(profile)
        val scoreCategory = viewModel.getMentalHealthCategory(mentalHealthScore)

        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            colors = CardDefaults.cardColors(
                containerColor = if (isDarkMode) DarkSurface else WarmCream
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Your Mental Health Score",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = accentColor,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "$mentalHealthScore/100",
                    fontSize = 36.sp,
                    fontWeight = FontWeight.Bold,
                    color = when (mentalHealthScore) {
                        in 85..100 -> Color(0xFF4CAF50) // Green
                        in 70..84 -> Color(0xFF8BC34A) // Light Green
                        in 55..69 -> Color(0xFFFF9800) // Orange
                        in 40..54 -> Color(0xFFFF5722) // Red Orange
                        else -> Color(0xFFF44336) // Red
                    },
                    modifier = Modifier.padding(bottom = 4.dp)
                )

                Text(
                    text = scoreCategory,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = textColor.copy(alpha = 0.8f),
                    textAlign = TextAlign.Center
                )

                if (mentalHealthScore < 70) {
                    Text(
                        text = "Consider speaking with a mental health professional",
                        fontSize = 14.sp,
                        color = textColor.copy(alpha = 0.7f),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
            }
        }

        // Submit Button
        Button(
            onClick = {
                viewModel.updateMentalHealthProfile(profile)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(28.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = accentColor,
                contentColor = Color.White
            ),
            enabled = true
        ) {
            Text(
                "✨ Save Assessment",
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            )
        }

        Spacer(modifier = Modifier.height(100.dp)) // Extra space for bottom nav
    }
}

@Preview(showBackground = true)
@Composable
fun DefaultPreview() {
    Startup_hackathon20Theme {
        MainApp()
    }
}
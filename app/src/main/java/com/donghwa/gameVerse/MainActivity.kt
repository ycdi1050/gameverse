package com.donghwa.gameVerse

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider

// 각 게임 뷰 import 확인
import com.donghwa.gameVerse.brickgame.BrickGameView
import com.donghwa.gameVerse.runnergame.RunnerGameView
import com.donghwa.gameVerse.simulation.CraneSimulationView
import com.donghwa.gameVerse.defensegame.DefenseGameView

class MainActivity : Activity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val rankingManager = RankingManager()

    private val RC_SIGN_IN = 9001
    private val WEB_CLIENT_ID = "588562798442-q2f8fsied1mdastv9rrjerahslnqohu6.apps.googleusercontent.com"

    // 각 게임 뷰 변수
    private var brickGameView: BrickGameView? = null
    private var runnerGameView: RunnerGameView? = null
    private var simulationView: CraneSimulationView? = null
    private var defenseGameView: DefenseGameView? = null

    private var myDefenseMaxStage = 1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setupFullScreen()
        auth = FirebaseAuth.getInstance()
        setupGoogleSignIn()

        val currentUser = auth.currentUser
        if (currentUser != null) {
            Toast.makeText(this, "접속 확인 중...", Toast.LENGTH_SHORT).show()
            showHomeScreen()
        } else {
            showLoginScreen()
        }
    }

    private fun setupFullScreen() {
        try {
            actionBar?.hide()
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                window.insetsController?.let {
                    it.hide(WindowInsets.Type.statusBars() or WindowInsets.Type.navigationBars())
                    it.systemBarsBehavior = WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
                }
            } else {
                @Suppress("DEPRECATION")
                window.decorView.systemUiVisibility = (
                        View.SYSTEM_UI_FLAG_FULLSCREEN or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
            }
        } catch (e: Exception) { e.printStackTrace() }
    }

    private fun setupGoogleSignIn() {
        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()
        googleSignInClient = GoogleSignIn.getClient(this, gso)
    }

    private fun showHomeScreen() {
        val user = auth.currentUser
        val uid = user?.uid ?: return

        rankingManager.loadHomeData(uid) { highScore, runnerHighScore, defenseHighScore, defenseMaxStage, leaderboard, runnerLeaderboard, defenseLeaderboard, level, currentXp, nickname ->

            this.myDefenseMaxStage = defenseMaxStage

            if (nickname.isNullOrEmpty()) {
                showNicknameSetupScreen(uid)
            } else {
                val homeView = HomeView(
                    this,
                    nickname,
                    highScore,
                    runnerHighScore,
                    defenseHighScore,
                    level,
                    currentXp,
                    leaderboard,
                    runnerLeaderboard,
                    defenseLeaderboard,
                    onStartBrickGame = { startBrickGame() },
                    onStartRunnerGame = { startRunnerGame() },
                    onStartSimulation = { startSimulation() },
                    onStartDefenseGame = { startDefenseGame() },
                    onLogout = { signOut() }
                )
                setContentView(homeView)
            }
        }
    }

    // --- [중요 수정] 누락된 게임 시작 함수 구현 ---

    private fun startBrickGame() {
        brickGameView = BrickGameView(this,
            onExit = {
                runOnUiThread {
                    brickGameView?.pause()
                    brickGameView = null
                    showHomeScreen()
                }
            },
            onGameOver = { score ->
                saveHighScore(score)
            }
        )
        setContentView(brickGameView)
        brickGameView?.resume()
    }

    private fun startRunnerGame() {
        runnerGameView = RunnerGameView(this,
            onExit = {
                runOnUiThread {
                    runnerGameView?.pause()
                    runnerGameView = null
                    showHomeScreen()
                }
            },
            onGameOver = { score ->
                saveRunnerHighScore(score)
            }
        )
        setContentView(runnerGameView)
        runnerGameView?.resume()
    }

    private fun startSimulation() {
        simulationView = CraneSimulationView(this) {
            runOnUiThread {
                simulationView = null
                showHomeScreen()
            }
        }
        setContentView(simulationView)
    }

    private fun startDefenseGame() {
        defenseGameView = DefenseGameView(
            this,
            maxUnlockedStage = myDefenseMaxStage,
            onExit = {
                runOnUiThread {
                    defenseGameView?.pause()
                    defenseGameView = null
                    showHomeScreen()
                }
            },
            onGameOver = { score, clearedStage ->
                saveDefenseHighScore(score, clearedStage)
            }
        )
        setContentView(defenseGameView)
        defenseGameView?.resume()
    }

    // --- [중요 수정] 누락된 점수 저장 및 로그아웃 구현 ---

    private fun saveHighScore(score: Int) {
        val user = auth.currentUser ?: return
        // RankingManager에 updateHighScore 메서드가 있다고 가정 (없으면 RankingManager도 확인 필요)
        rankingManager.updateHighScore(user.uid, user.displayName ?: "Player", score) {
            Toast.makeText(this, "벽돌 깨기 점수 저장 완료!", Toast.LENGTH_SHORT).show()
        }
        rankingManager.addExperience(user.uid, score) { level, up ->
            if (up) Toast.makeText(this, "레벨업! Lv.$level", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveRunnerHighScore(score: Int) {
        val user = auth.currentUser ?: return
        // RankingManager에 updateRunnerHighScore 메서드가 있다고 가정
        rankingManager.updateRunnerHighScore(user.uid, user.displayName ?: "Player", score) {
            Toast.makeText(this, "러닝 게임 점수 저장 완료!", Toast.LENGTH_SHORT).show()
        }
        rankingManager.addExperience(user.uid, score) { level, up ->
            if (up) Toast.makeText(this, "레벨업! Lv.$level", Toast.LENGTH_LONG).show()
        }
    }

    private fun saveDefenseHighScore(score: Int, clearedStage: Int) {
        val user = auth.currentUser ?: return
        rankingManager.updateDefenseHighScore(user.uid, user.displayName ?: "Player", score) {
            Toast.makeText(this, "디펜스 점수 저장 완료!", Toast.LENGTH_SHORT).show()
        }
        if (clearedStage > 0) {
            rankingManager.updateDefenseMaxStage(user.uid, clearedStage)
            if (clearedStage + 1 > myDefenseMaxStage) {
                myDefenseMaxStage = clearedStage + 1
            }
        }
        rankingManager.addExperience(user.uid, score) { level, up ->
            if (up) Toast.makeText(this, "레벨업! Lv.$level", Toast.LENGTH_LONG).show()
        }
    }

    private fun signOut() {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener(this) {
            showLoginScreen()
        }
    }

    // --- [중요 수정] 뒤로 가기 처리 (모든 게임에 적용) ---
    override fun onBackPressed() {
        when {
            brickGameView != null -> {
                brickGameView?.pause()
                brickGameView = null
                showHomeScreen()
            }
            runnerGameView != null -> {
                runnerGameView?.pause()
                runnerGameView = null
                showHomeScreen()
            }
            simulationView != null -> {
                simulationView = null
                showHomeScreen()
            }
            defenseGameView != null -> {
                defenseGameView?.pause()
                defenseGameView = null
                showHomeScreen()
            }
            else -> super.onBackPressed()
        }
    }

    // --- 기존 로그인/닉네임 설정 UI 함수들 ---

    private fun showNicknameSetupScreen(uid: String) {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setBackgroundColor(Color.parseColor("#121212"))
        layout.setPadding(50, 50, 50, 50)

        val title = TextView(this)
        title.text = "닉네임 설정"
        title.textSize = 30f
        title.setTextColor(Color.CYAN)
        title.gravity = Gravity.CENTER
        title.setPadding(0, 0, 0, 50)
        layout.addView(title)

        val input = EditText(this)
        input.hint = "닉네임"
        input.setTextColor(Color.WHITE)
        input.setHintTextColor(Color.GRAY)
        layout.addView(input)

        val confirmBtn = Button(this)
        confirmBtn.text = "확인"
        confirmBtn.setOnClickListener {
            val nick = input.text.toString().trim()
            if (nick.isNotEmpty()) {
                rankingManager.setNickname(uid, nick, { showHomeScreen() }, { Toast.makeText(this, it, Toast.LENGTH_SHORT).show() })
            }
        }
        layout.addView(confirmBtn)
        setContentView(layout)
    }

    private fun showLoginScreen() {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setBackgroundColor(Color.parseColor("#121212")) // 배경색 추가 권장

        val btn = Button(this)
        btn.text = "Google Login"
        btn.setOnClickListener { val signInIntent = googleSignInClient.signInIntent; startActivityForResult(signInIntent, RC_SIGN_IN) }
        layout.addView(btn)
        setContentView(layout)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                val credential = GoogleAuthProvider.getCredential(account.idToken!!, null)
                auth.signInWithCredential(credential).addOnCompleteListener {
                    if(it.isSuccessful) showHomeScreen()
                }
            } catch (e: ApiException) {}
        }
    }

    override fun onPause() {
        super.onPause()
        brickGameView?.pause()
        runnerGameView?.pause()
        defenseGameView?.pause()
    }

    override fun onResume() {
        super.onResume()
        brickGameView?.resume()
        runnerGameView?.resume()
        defenseGameView?.resume()
    }
}
// ... imports ...
package com.donghwa.gameVerse

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.InputFilter
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

import com.donghwa.gameVerse.brickgame.GameView
import com.donghwa.gameVerse.runnergame.RunnerGameView
import com.donghwa.gameVerse.simulation.CraneSimulationView
import com.donghwa.gameVerse.defensegame.DefenseGameView // 패키지 확인

class MainActivity : Activity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val rankingManager = RankingManager()

    private val RC_SIGN_IN = 9001
    private val WEB_CLIENT_ID = "588562798442-q2f8fsied1mdastv9rrjerahslnqohu6.apps.googleusercontent.com"

    private var brickGameView: GameView? = null
    private var runnerGameView: RunnerGameView? = null
    private var simulationView: CraneSimulationView? = null
    private var defenseGameView: DefenseGameView? = null

    // [신규] 디펜스 게임 최대 스테이지 저장 변수
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

        // [수정] 8번째 인자로 defenseMaxStage 수신
        rankingManager.loadHomeData(uid) { highScore, runnerHighScore, defenseHighScore, defenseMaxStage, leaderboard, runnerLeaderboard, defenseLeaderboard, level, currentXp, nickname ->

            this.myDefenseMaxStage = defenseMaxStage // 저장해둠

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
                    onStartDefenseGame = { startDefenseGame() }, // 호출
                    onLogout = { signOut() }
                )
                setContentView(homeView)
            }
        }
    }

    // ... (중략: showNicknameSetupScreen, showLoginScreen, signIn, onActivityResult 등 기존 코드 유지) ...
    // ... 중복을 줄이기 위해 생략합니다. 기존 코드를 그대로 두시면 됩니다 ...
    // 아래는 showNicknameSetupScreen 등 기존 함수들...

    private fun showNicknameSetupScreen(uid: String) {
        // ... (기존과 동일)
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
        // ... (기존과 동일)
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        val btn = Button(this)
        btn.text = "Google Login"
        btn.setOnClickListener { val signInIntent = googleSignInClient.signInIntent; startActivityForResult(signInIntent, RC_SIGN_IN) }
        layout.addView(btn)
        setContentView(layout)
    }

    private fun signIn() { /*...*/ } // 기존 코드 유지
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

    private fun startBrickGame() { /*...*/ }
    private fun startRunnerGame() { /*...*/ }
    private fun startSimulation() { /*...*/ }
    private fun saveHighScore(score: Int) { /*...*/ }
    private fun saveRunnerHighScore(score: Int) { /*...*/ }
    private fun signOut() { /*...*/ }

    // [수정] 디펜스 게임 시작 시 maxUnlockedStage 전달
    private fun startDefenseGame() {
        defenseGameView = DefenseGameView(
            this,
            maxUnlockedStage = myDefenseMaxStage, // 전달
            onExit = {
                runOnUiThread {
                    defenseGameView?.pause()
                    defenseGameView = null
                    showHomeScreen()
                }
            },
            onGameOver = { score, clearedStage -> // 클리어한 스테이지 정보도 받음
                saveDefenseHighScore(score, clearedStage)
            }
        )
        setContentView(defenseGameView)
        defenseGameView?.resume()
    }

    // [수정] 점수 및 스테이지 정보 저장
    private fun saveDefenseHighScore(score: Int, clearedStage: Int) {
        val user = auth.currentUser ?: return
        val uid = user.uid
        val name = user.displayName ?: "Player"

        // 1. 점수 저장
        rankingManager.updateDefenseHighScore(uid, name, score) {
            Toast.makeText(this, "디펜스 점수 저장 완료!", Toast.LENGTH_SHORT).show()
        }

        // 2. 최대 스테이지 갱신 (클리어 시)
        if (clearedStage > 0) {
            rankingManager.updateDefenseMaxStage(uid, clearedStage)
            // 로컬 변수도 갱신
            if (clearedStage + 1 > myDefenseMaxStage) {
                myDefenseMaxStage = clearedStage + 1
            }
        }

        // 3. 경험치
        rankingManager.addExperience(uid, score) { level, up ->
            if (up) Toast.makeText(this, "레벨업! Lv.$level", Toast.LENGTH_LONG).show()
        }
    }

    // 뒤로가기 처리 등 나머지 함수 유지
    override fun onBackPressed() {
        if (defenseGameView != null) {
            defenseGameView?.pause()
            defenseGameView = null
            showHomeScreen()
        } else {
            super.onBackPressed()
        }
    }

    override fun onPause() { super.onPause(); defenseGameView?.pause() }
    override fun onResume() { super.onResume(); defenseGameView?.resume() }
}
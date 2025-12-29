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

import com.donghwa.gameVerse.brickgame.BrickGameView
import com.donghwa.gameVerse.runnergame.RunnerGameView
import com.donghwa.gameVerse.simulation.CraneSimulationView
import com.donghwa.gameVerse.defensegame.DefenseGameView
import com.donghwa.gameVerse.defensegame.WeaponType
import com.donghwa.gameVerse.defensegame.DefenseCharacterType

class MainActivity : Activity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val rankingManager = RankingManager()

    private val RC_SIGN_IN = 9001
    private val WEB_CLIENT_ID = "588562798442-q2f8fsied1mdastv9rrjerahslnqohu6.apps.googleusercontent.com"

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

    // ... (화면 설정, 로그인 관련 코드는 기존과 동일하므로 생략하거나 유지)

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
                    // [수정] 캐릭터 타입, 무기 타입을 모두 전달받아 게임 시작
                    onStartDefenseGame = { charType, weaponType ->
                        startDefenseGame(charType, weaponType)
                    },
                    onLogout = { signOut() }
                )
                setContentView(homeView)
            }
        }
    }

    private fun startBrickGame() {
        brickGameView = BrickGameView(this,
            onExit = { runOnUiThread { brickGameView?.pause(); brickGameView = null; showHomeScreen() } },
            onGameOver = { score -> saveHighScore(score) }
        )
        setContentView(brickGameView)
        brickGameView?.resume()
    }

    private fun startRunnerGame() {
        runnerGameView = RunnerGameView(this,
            onExit = { runOnUiThread { runnerGameView?.pause(); runnerGameView = null; showHomeScreen() } },
            onGameOver = { score -> saveRunnerHighScore(score) }
        )
        setContentView(runnerGameView)
        runnerGameView?.resume()
    }

    private fun startSimulation() {
        simulationView = CraneSimulationView(this) { runOnUiThread { simulationView = null; showHomeScreen() } }
        setContentView(simulationView)
    }

    // [수정] 디펜스 게임 시작 (캐릭터, 무기 전달)
    private fun startDefenseGame(charType: DefenseCharacterType, weaponType: WeaponType) {
        defenseGameView = DefenseGameView(
            this,
            maxUnlockedStage = myDefenseMaxStage,
            initialWeapon = weaponType,
            initialCharacter = charType, // [신규]
            onExit = { runOnUiThread { defenseGameView?.pause(); defenseGameView = null; showHomeScreen() } },
            onGameOver = { score, clearedStage -> saveDefenseHighScore(score, clearedStage) }
        )
        setContentView(defenseGameView)
        defenseGameView?.resume()
    }

    // ... (점수 저장, 로그아웃 등 나머지 코드는 기존 유지)

    private fun saveHighScore(score: Int) {
        val user = auth.currentUser ?: return
        rankingManager.updateHighScore(user.uid, user.displayName ?: "Player", score) {}
        rankingManager.addExperience(user.uid, score) { _, _ -> }
    }

    private fun saveRunnerHighScore(score: Int) {
        val user = auth.currentUser ?: return
        rankingManager.updateRunnerHighScore(user.uid, user.displayName ?: "Player", score) {}
        rankingManager.addExperience(user.uid, score) { _, _ -> }
    }

    private fun saveDefenseHighScore(score: Int, clearedStage: Int) {
        val user = auth.currentUser ?: return
        rankingManager.updateDefenseHighScore(user.uid, user.displayName ?: "Player", score) {}
        if (clearedStage > 0) {
            rankingManager.updateDefenseMaxStage(user.uid, clearedStage)
            if (clearedStage + 1 > myDefenseMaxStage) myDefenseMaxStage = clearedStage + 1
        }
        rankingManager.addExperience(user.uid, score) { _, _ -> }
    }

    private fun signOut() {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener(this) { showLoginScreen() }
    }

    private fun showNicknameSetupScreen(uid: String) {
        val layout = LinearLayout(this)
        val input = EditText(this)
        val btn = Button(this)
        btn.text = "확인"
        btn.setOnClickListener {
            val nick = input.text.toString()
            if(nick.isNotEmpty()) rankingManager.setNickname(uid, nick, { showHomeScreen() }, {})
        }
        layout.addView(input)
        layout.addView(btn)
        setContentView(layout)
    }

    private fun showLoginScreen() {
        val layout = LinearLayout(this)
        val btn = Button(this)
        btn.text = "Login"
        btn.setOnClickListener { val intent = googleSignInClient.signInIntent; startActivityForResult(intent, RC_SIGN_IN) }
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
                auth.signInWithCredential(credential).addOnCompleteListener { if(it.isSuccessful) showHomeScreen() }
            } catch (e: ApiException) {}
        }
    }

    override fun onBackPressed() {
        if(brickGameView != null || runnerGameView != null || defenseGameView != null || simulationView != null) {
            brickGameView?.pause(); brickGameView = null
            runnerGameView?.pause(); runnerGameView = null
            defenseGameView?.pause(); defenseGameView = null
            simulationView = null
            showHomeScreen()
        } else {
            super.onBackPressed()
        }
    }
}
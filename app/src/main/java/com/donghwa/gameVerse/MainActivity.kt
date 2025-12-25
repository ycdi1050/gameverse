package com.donghwa.gameVerse // 패키지명 확인

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
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
import com.donghwa.gameVerse.managers.RankingManager

class MainActivity : Activity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val rankingManager = RankingManager()

    private val RC_SIGN_IN = 9001
    private val WEB_CLIENT_ID = "588562798442-q2f8fsied1mdastv9rrjerahslnqohu6.apps.googleusercontent.com"

    private var brickGameView: GameView? = null
    private var runnerGameView: RunnerGameView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

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
                        View.SYSTEM_UI_FLAG_FULLSCREEN
                                or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                                or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        )
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        auth = FirebaseAuth.getInstance()

        val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(WEB_CLIENT_ID)
            .requestEmail()
            .build()

        googleSignInClient = GoogleSignIn.getClient(this, gso)

        val currentUser = auth.currentUser
        if (currentUser != null) {
            Toast.makeText(this, "환영합니다! ${currentUser.displayName}님", Toast.LENGTH_SHORT).show()
            showHomeScreen()
        } else {
            showLoginScreen()
        }
    }

    private fun showHomeScreen() {
        val user = auth.currentUser
        val userName = user?.displayName ?: "Player"
        val uid = user?.uid

        if (uid != null) {
            // [수정] 4개의 파라미터를 받아와서 HomeView에 전달
            rankingManager.loadHomeData(uid) { highScore, runnerHighScore, leaderboard, runnerLeaderboard ->
                val homeView = HomeView(
                    this,
                    userName,
                    highScore,
                    runnerHighScore, // [신규]
                    leaderboard,
                    runnerLeaderboard, // [신규]
                    onStartBrickGame = { startBrickGame() },
                    onStartRunnerGame = { startRunnerGame() },
                    onLogout = { signOut() }
                )
                setContentView(homeView)
            }
        } else {
            showLoginScreen()
        }
    }

    // ... (showLoginScreen, signIn, onActivityResult, firebaseAuthWithGoogle 등은 기존과 동일) ...
    // 아래 코드는 위 MainActivity.kt 전체 코드에서 해당 부분만 붙여넣거나, 전체를 복사해서 쓰세요.
    // (분량 관계상 showHomeScreen 부분만 강조했습니다. 나머지 함수는 그대로 두시면 됩니다.)

    private fun showLoginScreen() {
        val layout = LinearLayout(this)
        layout.orientation = LinearLayout.VERTICAL
        layout.gravity = Gravity.CENTER
        layout.setBackgroundColor(Color.parseColor("#121212"))

        val title = TextView(this)
        title.text = "Game Verse"
        title.textSize = 40f
        title.setTextColor(Color.CYAN)
        title.gravity = Gravity.CENTER
        title.setPadding(0, 0, 0, 100)

        val loginBtn = Button(this)
        loginBtn.text = "G  구글 계정으로 로그인"
        loginBtn.textSize = 18f
        loginBtn.setBackgroundColor(Color.WHITE)
        loginBtn.setTextColor(Color.BLACK)
        loginBtn.setPadding(50, 30, 50, 30)

        loginBtn.setOnClickListener {
            signIn()
        }

        layout.addView(title)
        layout.addView(loginBtn)

        setContentView(layout)
    }

    private fun signIn() {
        val signInIntent = googleSignInClient.signInIntent
        startActivityForResult(signInIntent, RC_SIGN_IN)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == RC_SIGN_IN) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(data)
            try {
                val account = task.getResult(ApiException::class.java)!!
                firebaseAuthWithGoogle(account.idToken!!)
            } catch (e: ApiException) {
                Toast.makeText(this, "구글 로그인 실패: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun firebaseAuthWithGoogle(idToken: String) {
        val credential = GoogleAuthProvider.getCredential(idToken, null)
        auth.signInWithCredential(credential)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    val user = auth.currentUser
                    Toast.makeText(this, "로그인 성공: ${user?.displayName}", Toast.LENGTH_SHORT).show()
                    showHomeScreen()
                } else {
                    Toast.makeText(this, "인증 실패.", Toast.LENGTH_SHORT).show()
                }
            }
    }

    private fun startBrickGame() {
        brickGameView = GameView(
            this,
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

    private fun saveHighScore(score: Int) {
        val user = auth.currentUser
        val uid = user?.uid ?: return
        val userName = user.displayName ?: "Player"

        rankingManager.updateHighScore(uid, userName, score) { newScore ->
            Toast.makeText(this, "벽돌깨기 신기록! ($newScore)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveRunnerHighScore(score: Int) {
        val user = auth.currentUser
        val uid = user?.uid ?: return
        val userName = user.displayName ?: "Player"

        rankingManager.updateRunnerHighScore(uid, userName, score) { newScore ->
            Toast.makeText(this, "러닝 신기록! ($newScore)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun signOut() {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener(this) {
            Toast.makeText(this, "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show()
            brickGameView?.pause()
            brickGameView = null
            runnerGameView?.pause()
            runnerGameView = null
            showLoginScreen()
        }
    }

    override fun onBackPressed() {
        if (brickGameView != null && brickGameView!!.isShown) {
            brickGameView?.pause()
            brickGameView = null
            showHomeScreen()
        } else if (runnerGameView != null && runnerGameView!!.isShown) {
            runnerGameView?.pause()
            runnerGameView = null
            showHomeScreen()
        } else {
            super.onBackPressed()
        }
    }

    override fun onResume() {
        super.onResume()
        brickGameView?.resume()
        runnerGameView?.resume()
    }

    override fun onPause() {
        super.onPause()
        brickGameView?.pause()
        runnerGameView?.pause()
    }
}
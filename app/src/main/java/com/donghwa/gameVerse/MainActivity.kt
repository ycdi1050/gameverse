package com.donghwa.gameVerse

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowInsets
import android.view.WindowInsetsController
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
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
import com.donghwa.gameVerse.character.CharacterDataManager

import com.donghwa.gameVerse.defensegame.DefenseGameView
import com.donghwa.gameVerse.defensegame.WeaponType
import com.donghwa.gameVerse.defensegame.DefenseCharacterType
import com.donghwa.gameVerse.defensegame.WeaponGrade
import com.donghwa.gameVerse.defensegame.Difficulty

class MainActivity : Activity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var googleSignInClient: GoogleSignInClient
    private val rankingManager = RankingManager()
    private val characterDataManager = CharacterDataManager()

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
            Toast.makeText(this, "데이터를 불러오는 중...", Toast.LENGTH_SHORT).show()
            loadAllDataAndShowHome(currentUser.uid)
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

    private fun loadAllDataAndShowHome(uid: String) {
        rankingManager.loadHomeData(uid) { highScore, runnerHighScore, defenseHighScore, defenseMaxStage, leaderboard, runnerLeaderboard, defenseLeaderboard, level, currentXp, nickname ->
            this.myDefenseMaxStage = defenseMaxStage

            if (nickname.isNullOrEmpty()) {
                showNicknameSetupScreen(uid)
            } else {
                characterDataManager.loadDefenseInventory(uid) {
                    val homeView = HomeView(
                        this,
                        uid,
                        nickname,
                        highScore,
                        runnerHighScore,
                        defenseHighScore,
                        defenseMaxStage,
                        level,
                        currentXp,
                        leaderboard,
                        runnerLeaderboard,
                        defenseLeaderboard,
                        characterDataManager,
                        onStartBrickGame = { startBrickGame() },
                        onStartRunnerGame = { startRunnerGame() },
                        onStartSimulation = { startSimulation() },
                        onStartDefenseGame = { charType, weaponType, grade, stage, difficulty ->
                            startDefenseGame(charType, weaponType, grade, stage, difficulty)
                        },
                        onLogout = { signOut() }
                    )
                    setContentView(homeView)
                }
            }
        }
    }

    private fun startBrickGame() {
        brickGameView = BrickGameView(this,
            onExit = { runOnUiThread { brickGameView?.pause(); brickGameView = null; loadAllDataAndShowHome(auth.currentUser!!.uid) } },
            onGameOver = { score -> saveHighScore(score) }
        )
        setContentView(brickGameView)
        brickGameView?.resume()
    }

    private fun startRunnerGame() {
        runnerGameView = RunnerGameView(this,
            onExit = { runOnUiThread { runnerGameView?.pause(); runnerGameView = null; loadAllDataAndShowHome(auth.currentUser!!.uid) } },
            onGameOver = { score -> saveRunnerHighScore(score) }
        )
        setContentView(runnerGameView)
        runnerGameView?.resume()
    }

    private fun startSimulation() {
        simulationView = CraneSimulationView(this) { runOnUiThread { simulationView = null; loadAllDataAndShowHome(auth.currentUser!!.uid) } }
        setContentView(simulationView)
    }

    private fun startDefenseGame(charType: DefenseCharacterType, weaponType: WeaponType, grade: WeaponGrade, stage: Int, difficulty: Difficulty) {
        defenseGameView = DefenseGameView(
            this,
            maxUnlockedStage = myDefenseMaxStage,
            initialStage = stage,
            initialWeapon = weaponType,
            initialCharacter = charType,
            initialGrade = grade,
            initialDifficulty = difficulty,
            onExit = {
                val view = defenseGameView
                if (view != null) {
                    // [수정] consumeAcquiredDong 호출 (읽고 나서 0으로 초기화됨)
                    val acquiredDong = view.consumeAcquiredDong()
                    if (acquiredDong > 0) {
                        val user = auth.currentUser
                        if (user != null) {
                            characterDataManager.addDong(user.uid, acquiredDong)
                        }
                    }
                }

                runOnUiThread {
                    try {
                        if (!isFinishing && !isDestroyed) {
                            defenseGameView?.pause()
                            defenseGameView = null
                            val user = auth.currentUser
                            if (user != null) {
                                loadAllDataAndShowHome(user.uid)
                            } else {
                                showLoginScreen()
                            }
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                    }
                }
            },
            onGameOver = { score, clearedStage ->
                saveDefenseGameResult(score, clearedStage)
            },
            onItemCollected = { droppedWeapon, droppedGrade ->
                val user = auth.currentUser
                if (user != null) {
                    characterDataManager.unlockWeapon(user.uid, droppedWeapon, droppedGrade) { success ->
                        runOnUiThread { }
                    }
                }
            }
        )
        setContentView(defenseGameView)
        defenseGameView?.resume()
    }

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

    private fun saveDefenseGameResult(score: Int, clearedStage: Int) {
        val user = auth.currentUser ?: return

        rankingManager.updateDefenseHighScore(user.uid, user.displayName ?: "Player", score) {}

        val view = defenseGameView
        val currentDifficulty = view?.getDifficulty() ?: Difficulty.NORMAL
        val maxWave = view?.getCurrentWave() ?: 0
        // [수정] 클리어 여부와 상관없이 현재 스테이지 번호 가져오기
        val actualStage = view?.getStage() ?: 0

        // [수정] 실제 플레이한 스테이지가 유효하다면 웨이브 기록 저장
        if (actualStage > 0) {
            characterDataManager.recordStageClear(user.uid, actualStage, currentDifficulty, maxWave)
        }

        // 스테이지 해금 로직 (10웨이브 클리어 시)
        if (maxWave >= 10 && actualStage > 0) {
            val msg = "★Stage $actualStage [${currentDifficulty.label}] Cleared!★"
            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()

            if (actualStage == myDefenseMaxStage) {
                myDefenseMaxStage = actualStage + 1
                rankingManager.updateDefenseMaxStage(user.uid, actualStage)
            } else if (actualStage > myDefenseMaxStage) {
                myDefenseMaxStage = actualStage + 1
                rankingManager.updateDefenseMaxStage(user.uid, actualStage)
            }
        } else {
            Toast.makeText(this, "Wave $maxWave Reached", Toast.LENGTH_SHORT).show()
        }

        rankingManager.addExperience(user.uid, score) { _, _ -> }

        // [수정] 동 획득 및 소비 (중복 방지)
        val acquiredDong = view?.consumeAcquiredDong() ?: 0
        if (acquiredDong > 0) {
            characterDataManager.addDong(user.uid, acquiredDong)
        }
    }

    private fun signOut() {
        auth.signOut()
        googleSignInClient.signOut().addOnCompleteListener(this) { showLoginScreen() }
    }

    private fun showNicknameSetupScreen(uid: String) {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setPadding(50, 50, 50, 50)
            setBackgroundColor(android.graphics.Color.WHITE)
        }

        val title = android.widget.TextView(this).apply {
            text = "Welcome! Enter Nickname"
            textSize = 24f
            setTextColor(android.graphics.Color.BLACK)
            setPadding(0, 0, 0, 50)
        }
        layout.addView(title)

        val input = EditText(this).apply {
            hint = "Nickname"
            textSize = 18f
            setTextColor(android.graphics.Color.BLACK)
            setPadding(20, 20, 20, 20)
            setBackgroundColor(android.graphics.Color.LTGRAY)
        }
        layout.addView(input)

        val btn = Button(this).apply {
            text = "Start Game"
            textSize = 18f
            setPadding(20, 20, 20, 20)
            layoutParams = LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                topMargin = 50
            }

            setOnClickListener {
                val nick = input.text.toString()
                if(nick.isNotEmpty()) {
                    rankingManager.setNickname(uid, nick, {
                        loadAllDataAndShowHome(uid)
                    }, {
                        Toast.makeText(context, "Error saving nickname", Toast.LENGTH_SHORT).show()
                    })
                } else {
                    Toast.makeText(context, "Please enter a nickname", Toast.LENGTH_SHORT).show()
                }
            }
        }
        layout.addView(btn)

        setContentView(layout)
    }

    private fun showLoginScreen() {
        val layout = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(android.graphics.Color.parseColor("#121212"))
        }

        val title = android.widget.TextView(this).apply {
            text = "GAME VERSE"
            textSize = 40f
            setTextColor(android.graphics.Color.CYAN)
            typeface = android.graphics.Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, 100)
        }
        layout.addView(title)

        val btn = Button(this).apply {
            text = "Sign in with Google"
            textSize = 20f
            setPadding(50, 30, 50, 30)
            setOnClickListener { val intent = googleSignInClient.signInIntent; startActivityForResult(intent, RC_SIGN_IN) }
        }
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
                    if(it.isSuccessful) {
                        Toast.makeText(this, "데이터를 불러오는 중...", Toast.LENGTH_SHORT).show()
                        loadAllDataAndShowHome(auth.currentUser!!.uid)
                    } else {
                        Toast.makeText(this, "Authentication Failed.", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: ApiException) {
                Toast.makeText(this, "Google Sign In Failed", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onBackPressed() {
        if(brickGameView != null || runnerGameView != null || defenseGameView != null || simulationView != null) {
            brickGameView?.pause(); brickGameView = null
            runnerGameView?.pause(); runnerGameView = null
            defenseGameView?.pause(); defenseGameView = null
            simulationView = null
            loadAllDataAndShowHome(auth.currentUser!!.uid)
        } else {
            super.onBackPressed()
        }
    }
}
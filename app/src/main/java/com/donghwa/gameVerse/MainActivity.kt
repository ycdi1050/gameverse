package com.donghwa.gameVerse

import android.app.Activity
import android.content.Intent
import android.os.Bundle
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
                        level,
                        currentXp,
                        leaderboard,
                        runnerLeaderboard,
                        defenseLeaderboard,
                        characterDataManager,
                        onStartBrickGame = { startBrickGame() },
                        onStartRunnerGame = { startRunnerGame() },
                        onStartSimulation = { startSimulation() },
                        onStartDefenseGame = { charType, weaponType, grade ->
                            startDefenseGame(charType, weaponType, grade)
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

    private fun startDefenseGame(charType: DefenseCharacterType, weaponType: WeaponType, grade: WeaponGrade) {
        defenseGameView = DefenseGameView(
            this,
            maxUnlockedStage = myDefenseMaxStage,
            initialWeapon = weaponType,
            initialCharacter = charType,
            initialGrade = grade,
            onExit = {
                // [신규] 게임 종료(나가기) 시 획득한 동 저장
                val acquiredDong = defenseGameView?.getAcquiredDong() ?: 0
                if (acquiredDong > 0) {
                    val user = auth.currentUser
                    if (user != null) {
                        characterDataManager.addDong(user.uid, acquiredDong)
                    }
                }

                runOnUiThread {
                    defenseGameView?.pause()
                    defenseGameView = null
                    loadAllDataAndShowHome(auth.currentUser!!.uid)
                }
            },
            onGameOver = { score, clearedStage ->
                // 게임 오버/클리어 시 동 저장
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

        if (clearedStage > 0) {
            rankingManager.updateDefenseMaxStage(user.uid, clearedStage)
            if (clearedStage + 1 > myDefenseMaxStage) myDefenseMaxStage = clearedStage + 1
        }

        rankingManager.addExperience(user.uid, score) { _, _ -> }

        // [신규] 획득한 동 저장
        val acquiredDong = defenseGameView?.getAcquiredDong() ?: 0
        if (acquiredDong > 0) {
            characterDataManager.addDong(user.uid, acquiredDong)
        }
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
            if(nick.isNotEmpty()) rankingManager.setNickname(uid, nick, { loadAllDataAndShowHome(uid) }, {})
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
                auth.signInWithCredential(credential).addOnCompleteListener {
                    if(it.isSuccessful) {
                        Toast.makeText(this, "데이터를 불러오는 중...", Toast.LENGTH_SHORT).show()
                        loadAllDataAndShowHome(auth.currentUser!!.uid)
                    }
                }
            } catch (e: ApiException) {}
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
package com.donghwa.gameVerse

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

class RankingManager {
    private val db = FirebaseFirestore.getInstance()

    // [수정] defenseMaxStage(최대 잠금해제 스테이지) 추가 (총 8개 데이터 반환)
    fun loadHomeData(uid: String, onResult: (Int, Int, Int, Int, List<String>, List<String>, List<String>, Int, Int, String?) -> Unit) {

        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val myHighScore = document.getLong("highScore")?.toInt() ?: 0
                val myRunnerHighScore = document.getLong("runnerHighScore")?.toInt() ?: 0
                val myDefenseHighScore = document.getLong("defenseHighScore")?.toInt() ?: 0
                val myDefenseMaxStage = document.getLong("defenseMaxStage")?.toInt() ?: 1 // 기본값 1단계

                val myLevel = document.getLong("level")?.toInt() ?: 1
                val myXp = document.getLong("currentXp")?.toInt() ?: 0
                val myNickname = document.getString("nickname")

                // 랭킹 데이터 가져오기 (벽돌 -> 러닝 -> 디펜스 순차 호출)
                fetchBrickRanking { brickLeaderboard ->
                    fetchRunnerRanking { runnerLeaderboard ->
                        fetchDefenseRanking { defenseLeaderboard ->
                            onResult(
                                myHighScore, myRunnerHighScore, myDefenseHighScore, myDefenseMaxStage,
                                brickLeaderboard, runnerLeaderboard, defenseLeaderboard,
                                myLevel, myXp, myNickname
                            )
                        }
                    }
                }
            }
            .addOnFailureListener {
                onResult(0, 0, 0, 1, emptyList(), emptyList(), emptyList(), 1, 0, null)
            }
    }

    // 헬퍼 함수들로 분리하여 가독성 향상
    private fun fetchBrickRanking(onComplete: (List<String>) -> Unit) {
        db.collection("users").orderBy("highScore", Query.Direction.DESCENDING).limit(5).get()
            .addOnSuccessListener { result ->
                val list = result.mapNotNull {
                    val name = it.getString("userName") ?: "Unknown"
                    val score = it.getLong("highScore")?.toInt() ?: 0
                    if (score > 0) "$name : $score" else null
                }
                onComplete(list)
            }.addOnFailureListener { onComplete(emptyList()) }
    }

    private fun fetchRunnerRanking(onComplete: (List<String>) -> Unit) {
        db.collection("users").orderBy("runnerHighScore", Query.Direction.DESCENDING).limit(5).get()
            .addOnSuccessListener { result ->
                val list = result.mapNotNull {
                    val name = it.getString("userName") ?: "Unknown"
                    val score = it.getLong("runnerHighScore")?.toInt() ?: 0
                    if (score > 0) "$name : $score" else null
                }
                onComplete(list)
            }.addOnFailureListener { onComplete(emptyList()) }
    }

    private fun fetchDefenseRanking(onComplete: (List<String>) -> Unit) {
        db.collection("users").orderBy("defenseHighScore", Query.Direction.DESCENDING).limit(5).get()
            .addOnSuccessListener { result ->
                val list = result.mapNotNull {
                    val name = it.getString("userName") ?: "Unknown"
                    val score = it.getLong("defenseHighScore")?.toInt() ?: 0
                    if (score > 0) "$name : $score" else null
                }
                onComplete(list)
            }.addOnFailureListener { onComplete(emptyList()) }
    }

    // [신규] 디펜스 최대 스테이지 갱신
    fun updateDefenseMaxStage(uid: String, clearedStage: Int) {
        val userRef = db.collection("users").document(uid)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            val currentMax = snapshot.getLong("defenseMaxStage")?.toInt() ?: 1
            // 다음 스테이지(clearedStage + 1)를 해금
            if (clearedStage + 1 > currentMax) {
                transaction.update(userRef, "defenseMaxStage", clearedStage + 1)
            }
        }
    }

    // --- 기존 함수들 유지 ---
    fun setNickname(uid: String, nickname: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        val userRef = db.collection("users").document(uid)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            if (snapshot.exists() && !snapshot.getString("nickname").isNullOrEmpty()) {
                throw Exception("ALREADY_SET")
            }
            val data = hashMapOf("nickname" to nickname, "userName" to nickname)
            transaction.set(userRef, data, SetOptions.merge())
        }.addOnSuccessListener { onSuccess() }
            .addOnFailureListener { e -> onError(if (e.message == "ALREADY_SET") "이미 닉네임이 설정됨" else e.message ?: "Error") }
    }

    fun addExperience(uid: String, earnedXp: Int, onLevelUp: (Int, Boolean) -> Unit) {
        val userRef = db.collection("users").document(uid)
        db.runTransaction { transaction ->
            val snapshot = transaction.get(userRef)
            var level = snapshot.getLong("level")?.toInt() ?: 1
            var xp = (snapshot.getLong("currentXp")?.toInt() ?: 0) + earnedXp
            var required = level * 100
            var leveledUp = false
            while (xp >= required) {
                xp -= required
                level++
                required = level * 100
                leveledUp = true
            }
            transaction.update(userRef, "level", level, "currentXp", xp)
            Pair(level, leveledUp)
        }.addOnSuccessListener { onLevelUp(it.first, it.second) }
    }

    fun updateHighScore(uid: String, userName: String, newScore: Int, onNewRecord: (Int) -> Unit) = updateScore(uid, userName, "highScore", newScore, onNewRecord)
    fun updateRunnerHighScore(uid: String, userName: String, newScore: Int, onNewRecord: (Int) -> Unit) = updateScore(uid, userName, "runnerHighScore", newScore, onNewRecord)
    fun updateDefenseHighScore(uid: String, userName: String, newScore: Int, onNewRecord: (Int) -> Unit) = updateScore(uid, userName, "defenseHighScore", newScore, onNewRecord)

    private fun updateScore(uid: String, userName: String, field: String, newScore: Int, onNewRecord: (Int) -> Unit) {
        db.collection("users").document(uid).get().addOnSuccessListener { doc ->
            val current = doc.getLong(field)?.toInt() ?: 0
            val name = doc.getString("nickname") ?: userName
            if (newScore > current) {
                db.collection("users").document(uid).set(hashMapOf(field to newScore, "userName" to name), SetOptions.merge())
                    .addOnSuccessListener { onNewRecord(newScore) }
            }
        }
    }
}
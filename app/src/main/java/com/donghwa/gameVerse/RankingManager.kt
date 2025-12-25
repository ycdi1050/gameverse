package com.donghwa.gameVerse.managers // 패키지명 확인

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.SetOptions

class RankingManager {
    private val db = FirebaseFirestore.getInstance()

    // [수정] 콜백에 runnerLeaderboard(List<String>) 추가
    fun loadHomeData(uid: String, onResult: (Int, Int, List<String>, List<String>) -> Unit) {

        // 1. 내 점수 정보 가져오기
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val myHighScore = document.getLong("highScore")?.toInt() ?: 0
                val myRunnerHighScore = document.getLong("runnerHighScore")?.toInt() ?: 0

                // 2. 벽돌깨기 랭킹 가져오기
                db.collection("users")
                    .orderBy("highScore", Query.Direction.DESCENDING)
                    .limit(5)
                    .get()
                    .addOnSuccessListener { brickResult ->
                        val brickLeaderboard = ArrayList<String>()
                        for (doc in brickResult) {
                            val name = doc.getString("userName") ?: "Unknown"
                            val score = doc.getLong("highScore")?.toInt() ?: 0
                            if (score > 0) brickLeaderboard.add("$name : $score")
                        }

                        // 3. [신규] 러닝 게임 랭킹 가져오기 (이어서 실행)
                        db.collection("users")
                            .orderBy("runnerHighScore", Query.Direction.DESCENDING)
                            .limit(5)
                            .get()
                            .addOnSuccessListener { runnerResult ->
                                val runnerLeaderboard = ArrayList<String>()
                                for (doc in runnerResult) {
                                    val name = doc.getString("userName") ?: "Unknown"
                                    val score = doc.getLong("runnerHighScore")?.toInt() ?: 0
                                    if (score > 0) runnerLeaderboard.add("$name : $score")
                                }

                                // [결과] 모든 데이터 반환 (벽돌 랭킹 + 러닝 랭킹)
                                onResult(myHighScore, myRunnerHighScore, brickLeaderboard, runnerLeaderboard)
                            }
                            .addOnFailureListener {
                                // 러닝 랭킹 실패 시 벽돌 랭킹만이라도 반환
                                onResult(myHighScore, myRunnerHighScore, brickLeaderboard, emptyList())
                            }
                    }
                    .addOnFailureListener {
                        onResult(myHighScore, myRunnerHighScore, emptyList(), emptyList())
                    }
            }
            .addOnFailureListener {
                onResult(0, 0, emptyList(), emptyList())
            }
    }

    // 벽돌깨기 점수 저장
    fun updateHighScore(uid: String, userName: String, newScore: Int, onNewRecord: (Int) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val currentHigh = document.getLong("highScore")?.toInt() ?: 0

                if (newScore > currentHigh) {
                    val data = hashMapOf(
                        "highScore" to newScore,
                        "userName" to userName
                    )
                    db.collection("users").document(uid).set(data, SetOptions.merge())
                        .addOnSuccessListener {
                            onNewRecord(newScore)
                        }
                }
            }
    }

    // 러닝 게임 점수 저장
    fun updateRunnerHighScore(uid: String, userName: String, newScore: Int, onNewRecord: (Int) -> Unit) {
        db.collection("users").document(uid).get()
            .addOnSuccessListener { document ->
                val currentHigh = document.getLong("runnerHighScore")?.toInt() ?: 0

                if (newScore > currentHigh) {
                    val data = hashMapOf(
                        "runnerHighScore" to newScore,
                        "userName" to userName
                    )
                    db.collection("users").document(uid).set(data, SetOptions.merge())
                        .addOnSuccessListener {
                            onNewRecord(newScore)
                        }
                }
            }
    }
}
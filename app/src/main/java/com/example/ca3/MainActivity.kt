package com.example.ca3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.room.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.Flow

// 1. ENTITY
@Entity(tableName = "students")
data class Student(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val name: String,
    val age: Int
)

// 2. DAO
@Dao
interface StudentDao {

    @Insert
    suspend fun insert(student: Student)

    @Query("SELECT * FROM students")
    fun getAllStudents(): Flow<List<Student>>

    @Update
    suspend fun update(student: Student)

    @Delete
    suspend fun delete(student: Student)
}

// 3. DATABASE
@Database(
    entities = [Student::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun studentDao(): StudentDao
}

// 4. MAIN ACTIVITY
class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val db = Room.databaseBuilder(
            applicationContext,
            AppDatabase::class.java,
            "student_database"
        ).build()

        setContent {
            MaterialTheme {
                StudentScreen(db.studentDao())
            }
        }
    }
}

// 5. UI
@Composable
fun StudentScreen(dao: StudentDao) {

    val students by dao.getAllStudents()
        .collectAsState(initial = emptyList())

    val scope = rememberCoroutineScope()

    var name by remember { mutableStateOf("") }
    var age by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {

        Text("Student Management",
            style = MaterialTheme.typography.headlineSmall)

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Student Name") }
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = age,
            onValueChange = { age = it },
            label = { Text("Student Age") }
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(onClick = {
            val studentAge = age.toIntOrNull()

            if (name.isNotBlank() && studentAge != null) {
                scope.launch {
                    dao.insert(
                        Student(name = name, age = studentAge)
                    )
                    name = ""
                    age = ""
                }
            }
        }) {
            Text("Add Student")
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text("Student List")

        LazyColumn {
            items(students, key = { it.id }) { student ->

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${student.name} - ${student.age}")

                    TextButton(onClick = {
                        scope.launch {
                            dao.delete(student)
                        }
                    }) {
                        Text("Delete")
                    }
                }
            }
        }
    }
}

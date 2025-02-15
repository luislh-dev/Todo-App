package com.example.todoapp.settings.drive.data

import android.util.Log
import com.example.todoapp.addtasks.data.TaskEntity
import com.example.todoapp.taskcategory.data.CategoryEntity
import com.example.todoapp.utils.Logger
import com.google.api.client.http.InputStreamContent
import com.google.api.services.drive.Drive
import com.google.api.services.drive.model.File
import com.google.gson.Gson
import com.google.gson.JsonObject
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

class GoogleDriveRepository @Inject constructor() {
    fun getDrive(accessToken: String): Drive = getDriveService(accessToken)

    fun searchFileInDrive(driveService: Drive, entityId: String, type: String): File? {
        val query =
            "name='$entityId' and mimeType='application/json' and properties has { key='type' and value='$type' }"
        return executeDriveQuery(driveService, query)?.firstOrNull()
    }

    fun downloadFileContent(driveService: Drive, fileId: String): String? {
        return executeDriveAction {
            val inputStream = driveService.files().get(fileId).executeMediaAsInputStream()
            inputStream.bufferedReader().use { it.readText() }
        }
    }

    fun updateFileInDrive(driveService: Drive, fileId: String, newContent: String) {
        val contentStream =
            InputStreamContent("application/json", newContent.toByteArray().inputStream())
        executeDriveAction {
            driveService.files().update(fileId, null, contentStream).setFields("id, name").execute()
            logMessage("Archivo actualizado en Google Drive: ID=$fileId")
        }
    }

    fun createFileInDrive(
        driveService: Drive,
        entityId: String,
        entityUpdateAt: OffsetDateTime,
        type: EntityType,
        content: String,
    ) {
        val fileMetadata = File().apply {
            name = entityId
            mimeType = "application/json"
            parents = listOf("appDataFolder")
            properties = mapOf("type" to type.value, "updatedAt" to entityUpdateAt.toString())
        }
        val contentStream = InputStreamContent("application/json", content.toByteArray().inputStream())
        executeDriveAction {
            val file = driveService.files().create(fileMetadata, contentStream)
                .setFields("id, name, properties").execute()
            logMessage("${type.name} guardada en Google Drive: ID=${file.id}, Nombre=${file.name}")
        }
    }

    fun deleteFileInDrive(driveService: Drive, fileId: String) {
        executeDriveAction {
            driveService.files().delete(fileId).execute()
            logMessage("Archivo eliminado en Google Drive: ID=$fileId")
        }
    }

    fun clearAppDataFromGoogleDrive(driveService: Drive) {
        val files =
            executeDriveQuery(driveService, "mimeType='application/json'") ?: return
        logMessage("Archivos encontrados para eliminar: ${files.size}")
        files.forEach { executeDriveAction { driveService.files().delete(it.id).execute() } }
    }

    fun countFilesInDrive(driveService: Drive): Int {
        return executeDriveAction {
            driveService.files().list().setSpaces("appDataFolder").setFields("files(id)").execute()
                .files.size
        } ?: 0
    }

    private fun logMessage(message: String) {
        Log.d("GoogleDriveRepository", message)
    }



    private fun <T> executeDriveAction(action: () -> T): T? {
        return try {
            action()
        } catch (e: Exception) {
            Logger.error(
                "GoogleDriveRepository",
                "Error al ejecutar acción en Google Drive: $e",
            )
            null
        }
    }

    private fun executeDriveQuery(driveService: Drive, query: String): List<File>? {
        return executeDriveAction {
            driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ(query)
                .setFields("files(id, name, properties)")
                .execute()
                .files
        }
    }

    enum class EntityType(val value: String) {
        TASK("task"),
        CATEGORY("category")
    }

    fun getAllTasksAndCategories(driveService: Drive): Pair<List<TaskEntity>, List<CategoryEntity>> {
        Log.d("GoogleDriveRepository", "getAllTasksAndCategories")
        val tasks = mutableListOf<TaskEntity>()
        val categories = mutableListOf<CategoryEntity>()

        val taskFiles = executeDriveQuery(driveService, "properties has { key='type' and value='${EntityType.TASK.value}' }")
        val categoryFiles = executeDriveQuery(driveService, "properties has { key='type' and value='${EntityType.CATEGORY.value}' }")

        taskFiles?.forEach { file ->
            val json = downloadFileContent(driveService, file.id)
            json?.let {
                val taskEntity = Gson().fromJson(json, TaskEntity::class.java)
                Log.d("GoogleDriveRepository", "Task entity: $taskEntity")
                tasks.add(taskEntity)
            }
        }

        categoryFiles?.forEach { file ->
            val json = downloadFileContent(driveService, file.id)
            json?.let {
                // Convertir el JSON a un objeto CategoryEntity
                val jsonObject = Gson().fromJson(it, JsonObject::class.java)
                // Extraer el objeto categoryEntity del JSON
                val categoryJson = jsonObject.getAsJsonObject("categoryEntity")
                // Convertir el objeto JSON a un objeto CategoryEntity
                val categoryEntity = Gson().fromJson(categoryJson, CategoryEntity::class.java)
                Log.d("GoogleDriveRepository", "Category entity: $categoryEntity")
                categories.add(categoryEntity)
            }

        }

        return Pair(tasks, categories)
    }

}

sealed class DriveEntity {
    data class Task(val taskEntity: TaskEntity) : DriveEntity()
    data class Category(val categoryEntity: CategoryEntity) : DriveEntity()
}
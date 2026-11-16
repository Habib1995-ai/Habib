package com.example.data.model

import org.json.JSONArray
import org.json.JSONObject

object Scene3DJsonCodec {

    fun toJsonObject(spec: Scene3DSpec): JSONObject {
        val obj = JSONObject()
        obj.put("sceneType", spec.sceneType.name)
        obj.put("shapeVariant", spec.shapeVariant)
        obj.put("title", spec.title)
        obj.put("spatialSummary", spec.spatialSummary)
        obj.put("interactionHint", spec.interactionHint)
        obj.put("formulaLabel", spec.formulaLabel)

        val nodesArr = JSONArray()
        spec.nodes.forEach { n ->
            val nObj = JSONObject()
            nObj.put("id", n.id)
            nObj.put("label", n.label)
            nObj.put("x", n.x.toDouble())
            nObj.put("y", n.y.toDouble())
            nObj.put("z", n.z.toDouble())
            nObj.put("radius", n.radius.toDouble())
            nObj.put("colorHex", n.colorHex)
            nObj.put("orbitRadius", n.orbitRadius.toDouble())
            nObj.put("orbitSpeed", n.orbitSpeed.toDouble())
            nObj.put("orbitTiltDeg", n.orbitTiltDeg.toDouble())
            nodesArr.put(nObj)
        }
        obj.put("nodes", nodesArr)

        val edgesArr = JSONArray()
        spec.edges.forEach { e ->
            val eObj = JSONObject()
            eObj.put("fromIndex", e.fromIndex)
            eObj.put("toIndex", e.toIndex)
            eObj.put("label", e.label)
            eObj.put("isVector", e.isVector)
            eObj.put("isDashed", e.isDashed)
            eObj.put("bondOrder", e.bondOrder)
            eObj.put("colorHex", e.colorHex)
            edgesArr.put(eObj)
        }
        obj.put("edges", edgesArr)

        val focusesArr = JSONArray()
        spec.stepFocuses.forEach { sf ->
            val sObj = JSONObject()
            sObj.put("stepNumber", sf.stepNumber)
            sObj.put("visualExplanation", sf.visualExplanation)
            sObj.put("focusNodeIndices", JSONArray(sf.focusNodeIndices))
            sObj.put("focusEdgeIndices", JSONArray(sf.focusEdgeIndices))
            sObj.put("suggestedYawDeg", sf.suggestedYawDeg.toDouble())
            sObj.put("suggestedPitchDeg", sf.suggestedPitchDeg.toDouble())
            focusesArr.put(sObj)
        }
        obj.put("stepFocuses", focusesArr)

        return obj
    }

    fun toJsonString(spec: Scene3DSpec): String = toJsonObject(spec).toString()

    fun fromJsonString(json: String?): Scene3DSpec? {
        if (json.isNullOrBlank()) return null
        return try {
            fromJsonObject(JSONObject(json))
        } catch (_: Exception) {
            null
        }
    }

    fun fromJsonObject(obj: JSONObject?): Scene3DSpec? {
        if (obj == null) return null
        val typeRaw = obj.optString("sceneType", Scene3DType.MATH_SURFACE_3D.name)
            .uppercase()
            .trim()
        val sceneType = when {
            typeRaw.contains("ASTRONOMY") || typeRaw.contains("ASTRO") || typeRaw.contains("SPACE") ->
                Scene3DType.ASTRONOMY_ASTROPHYSICS
            typeRaw.contains("GEOMETRY") || typeRaw.contains("SOLID") ->
                Scene3DType.GEOMETRY_SOLID
            typeRaw.contains("CHEM") || typeRaw.contains("MOLECULE") ->
                Scene3DType.CHEMISTRY_MOLECULE
            typeRaw.contains("PHYS") || typeRaw.contains("MECHANICS") ->
                Scene3DType.PHYSICS_MECHANICS
            else -> runCatching { Scene3DType.valueOf(typeRaw) }.getOrDefault(Scene3DType.MATH_SURFACE_3D)
        }

        val shapeVariant = obj.optString("shapeVariant", "").ifBlank { "CUSTOM_GRAPH" }
        val title = obj.optString("title", "مدل سه‌بعدی تعاملی")
        val spatialSummary = obj.optString("spatialSummary", "")
        val interactionHint = obj.optString(
            "interactionHint",
            "با انگشت مدل را بچرخانید و با دو انگشت بزرگ‌نمایی کنید"
        )
        val formulaLabel = obj.optString("formulaLabel", "")

        val nodes = mutableListOf<Node3D>()
        val nodesArr = obj.optJSONArray("nodes")
        if (nodesArr != null) {
            for (i in 0 until nodesArr.length()) {
                val nObj = nodesArr.optJSONObject(i) ?: continue
                nodes.add(
                    Node3D(
                        id = nObj.optInt("id", i),
                        label = nObj.optString("label", ""),
                        x = nObj.optDouble("x", 0.0).toFloat().coerceIn(-5f, 5f),
                        y = nObj.optDouble("y", 0.0).toFloat().coerceIn(-5f, 5f),
                        z = nObj.optDouble("z", 0.0).toFloat().coerceIn(-5f, 5f),
                        radius = nObj.optDouble("radius", 0.22).toFloat().coerceIn(0.06f, 1.2f),
                        colorHex = nObj.optString("colorHex", "#3B82F6").ifBlank { "#3B82F6" },
                        orbitRadius = nObj.optDouble("orbitRadius", 0.0).toFloat().coerceIn(0f, 5f),
                        orbitSpeed = nObj.optDouble("orbitSpeed", 0.0).toFloat().coerceIn(-4f, 4f),
                        orbitTiltDeg = nObj.optDouble("orbitTiltDeg", 0.0).toFloat().coerceIn(-89f, 89f)
                    )
                )
            }
        }

        val edges = mutableListOf<Edge3D>()
        val edgesArr = obj.optJSONArray("edges")
        if (edgesArr != null) {
            for (i in 0 until edgesArr.length()) {
                val eObj = edgesArr.optJSONObject(i) ?: continue
                edges.add(
                    Edge3D(
                        fromIndex = eObj.optInt("fromIndex", 0),
                        toIndex = eObj.optInt("toIndex", 0),
                        label = eObj.optString("label", ""),
                        isVector = eObj.optBoolean("isVector", false),
                        isDashed = eObj.optBoolean("isDashed", false),
                        bondOrder = eObj.optInt("bondOrder", 1).coerceIn(1, 3),
                        colorHex = eObj.optString("colorHex", "#94A3B8").ifBlank { "#94A3B8" }
                    )
                )
            }
        }

        val focuses = mutableListOf<Step3DFocus>()
        val fArr = obj.optJSONArray("stepFocuses")
        if (fArr != null) {
            for (i in 0 until fArr.length()) {
                val fObj = fArr.optJSONObject(i) ?: continue
                val fNodes = mutableListOf<Int>()
                val fnArr = fObj.optJSONArray("focusNodeIndices")
                if (fnArr != null) {
                    for (j in 0 until fnArr.length()) fNodes.add(fnArr.optInt(j))
                }
                val fEdges = mutableListOf<Int>()
                val feArr = fObj.optJSONArray("focusEdgeIndices")
                if (feArr != null) {
                    for (j in 0 until feArr.length()) fEdges.add(feArr.optInt(j))
                }
                focuses.add(
                    Step3DFocus(
                        stepNumber = fObj.optInt("stepNumber", i + 1),
                        visualExplanation = fObj.optString("visualExplanation", ""),
                        focusNodeIndices = fNodes,
                        focusEdgeIndices = fEdges,
                        suggestedYawDeg = fObj.optDouble("suggestedYawDeg", 35.0).toFloat(),
                        suggestedPitchDeg = fObj.optDouble("suggestedPitchDeg", 22.0).toFloat()
                    )
                )
            }
        }

        return Scene3DSpec(
            sceneType = sceneType,
            shapeVariant = shapeVariant,
            title = title,
            spatialSummary = spatialSummary,
            interactionHint = interactionHint,
            formulaLabel = formulaLabel,
            nodes = nodes,
            edges = edges,
            stepFocuses = focuses
        )
    }
}

package com.example.landmarkremark

class MarkerData {
    var latitude = 0.0
        private set
    var longitude = 0.0
        private set
    var name: String? = null
        private set
    var content: String? = null
        private set

    constructor()
    constructor(latitude: Double, longitude: Double, name: String?, content: String?) {
        this.latitude = latitude
        this.longitude = longitude
        this.name = name
        this.content = content
    }
}

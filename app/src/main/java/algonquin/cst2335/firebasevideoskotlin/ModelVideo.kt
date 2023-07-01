package algonquin.cst2335.firebasevideoskotlin

class ModelVideo {
    //variables, use same names as in firebase
    var id:String? = null
    var title:String?=null
    var timestamp:String? = null
    var videoUri:String?=null

    //empty constructor
    constructor(){

    }

    constructor(id: String?, title: String?, timestamp: String?, videoUri: String?) {
        this.id = id
        this.title = title
        this.timestamp = timestamp
        this.videoUri = videoUri
    }


}
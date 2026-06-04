package dev.favourdevlabs.cleanthes.data.entities

data
class VaultEntry(
var id:Long=0,
var title:String="",
var username:String="",
var encryptedPassword:String="",
var website:String?=null,
var category:String="General",
var notes:String?=null,
var createdAt:Long=0,
var updatedAt:Long=0,
var isFavorite:Boolean=false,
// TOTP fields — null totpSecret = no TOTP on this entry
var totpSecret:String?=null,
var totpIssuer:String?=null,
var totpDigits:Int=6,
var totpPeriod:Int=30,
var totpAlgorithm:String="SHA1" // SHA1 | SHA256 | SHA512
)
{

    fun hasTOTP(): Boolean = !totpSecret.isNullOrEmpty()

    override fun toString(): String =
        "VaultEntry{id=$id, title='$title', hasTOTP=${hasTOTP()}}"
}

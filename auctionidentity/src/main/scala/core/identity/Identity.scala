package core.identity

case class File(url: String, lastModified: Long)
case class AuctionStatus(files: Seq[File])

case class Realm(name: String, slug: String)
case class Auction(auc: Long, item: Long, owner: String, ownerRealm: String, bid: Long, buyout: Long, quantity: Long, timeLeft: String, rand: Long, seed:Long, context:Long)
case class AuctionData(realms: Seq[Realm], auctions: Seq[Auction])
case class AuctionWindow(item: Long, accumAvg: Long, lastAvg: Long)
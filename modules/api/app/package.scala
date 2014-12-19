package object auth {

  import play.api.mvc.RequestHeader
  import user.common.{UserService => us}

  /**
   * Scala LinkedHashMap delegator.
   */
  val LinkedHashMap = collection.mutable.LinkedHashMap

  /**
   * Get login userId by HTTP request.
   */
  def auth(request: RequestHeader) = us.getLoginUserId(request)

}
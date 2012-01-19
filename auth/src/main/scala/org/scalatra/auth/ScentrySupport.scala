package org.scalatra.auth

import javax.servlet.http.{HttpServletRequest, HttpServletResponse}
import util.DynamicVariable
import javax.servlet.{FilterConfig, ServletConfig}
import org.scalatra.{CookieSupport, Initializable, Handler, ScalatraKernel}

trait ScentryConfig {
  val login = "/login"
  val returnTo = "/"
  val returnToKey = "returnTo"
  val failureUrl = "/unauthenticated"
}

trait ScentrySupport[TypeForUser <: AnyRef] extends Handler with Initializable with CookieSupport {
  self : ScalatraKernel =>

  type UserType = TypeForUser
  type ScentryConfiguration <: ScentryConfig

  protected def fromSession: PartialFunction[String, UserType]
  protected def toSession: PartialFunction[UserType, String]
  protected def scentryConfig: ScentryConfiguration

  private var _strategiesFromConfig = List[String]()

  abstract override def initialize(config: Config) {
    super.initialize(config)
    readStrategiesFromConfig(config)
  }

  abstract override def handle(servletRequest: HttpServletRequest, servletResponse: HttpServletResponse) = {
    _request.withValue(servletRequest) {
      request(Scentry.ScentryRequestKey) = new Scentry[UserType](self, toSession, fromSession)
      configureScentry
      registerStrategiesFromConfig
      registerAuthStrategies
      super.handle(servletRequest, servletResponse)
    }
  }

  private def readStrategiesFromConfig(config: Config) = _strategiesFromConfig = {
    val strats = (config match {
      case servletConfig: ServletConfig => {
        servletConfig.getInitParameter("scentry.strategies")
      }
      case filterConfig: FilterConfig =>
        filterConfig.getInitParameter("scentry.strategies")
      case _ => ""
    })
    if(strats != null && strats.trim.nonEmpty) (strats split ";").toList else Nil
  }

  private def registerStrategiesFromConfig = _strategiesFromConfig foreach { strategyClassName =>
    val strategy = Class.forName(strategyClassName).newInstance.asInstanceOf[ScentryStrategy[UserType]]
    strategy registerWith scentry
  }

  protected def configureScentry = {

  }

  /**
   * Override this method to register authentication strategies specific to this servlet.
   *     registerAuthStrategy('UserPassword, app => new UserPasswordStrategy(app))
   */
  protected def registerAuthStrategies = {

  }

  protected def scentry: Scentry[UserType] = request(Scentry.ScentryRequestKey).asInstanceOf[Scentry[UserType]]
  protected def scentryOption: Option[Scentry[UserType]] = Option(request(Scentry.ScentryRequestKey)).map(_.asInstanceOf[Scentry[UserType]])
  protected def userOption: Option[UserType] = scentry.userOption
  implicit protected def user: UserType = scentry.user
  protected def user_=(user: UserType) = scentry.user = user
  protected def isAuthenticated : Boolean = scentry.isAuthenticated
  protected def isAnonymous : Boolean = !isAuthenticated
  @deprecated("use isAuthenticated")
  protected def authenticated_? : Boolean = isAuthenticated
  @deprecated("use !isAuthenticated")
  protected def unAuthenticated_? : Boolean = !isAuthenticated

  protected def authenticate() = {
    scentry.authenticate()
  }

  protected def logOut() = scentry.logout()

  @deprecated("use logOut()")
  protected def logOut_! = logOut()
}

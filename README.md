# Aplikasi Single Sign On

#Setup Development Environment
- JDK 1.8
- Tomcat Sever
- MySQL

#Teknologi yang digunakan
- Spring Boot
- Spring Security
- AngularJS

#Setup Project
- Buka browser masukkan url
  ```
    http://start.spring.io/
  ```
- Masukkan data, sesuaikan dengan dependency yang dibutuhkan(web, security) lalu download
- Add project ke text editor

Note :
Pada Project tersebut terdapat 3 buah file utama yaitu :
- pom.xml (konfigurasi maven)
- application.properties (konfigurasi database)
- Application.java (main class)

#Simple Oauth Facebook
- Konfigurasi Oauth (application.yml)
  Note : application.properties diganti application.yml
  ```
    security:
      oauth2:
        client:
          clientId: 233668646673605
          clientSecret: 33b17e044ee6a4fa383f46ec6e28ea1d
          accessTokenUri: https://graph.facebook.com/oauth/access_token
          userAuthorizationUri: https://www.facebook.com/dialog/oauth
          tokenName: oauth_token
          authenticationScheme: query
          clientAuthenticationScheme: form
        resource:
          userInfoUri: https://graph.facebook.com/me
  ```
- Kasih anotasi @EnableOAuth2Sso (Aplikasi.java)
  ```
    @EnableOAuth2Sso
    @SpringBootApplication
    public class Aplikasi {
    	public static void main(String[] args) {
    		SpringApplication.run(Aplikasi.class, args);
    	}
    }
  ```
- Bikin UI jika otorisari succes(resource/static/index.html)
  ```
    <!doctype html>
    <html lang="en">
    <head>
        <meta charset="utf-8" />
        <meta http-equiv="X-UA-Compatible" content="IE=edge" />
        <title>Demo</title>
        <meta name="description" content="" />
        <meta name="viewport" content="width=device-width" />
        <base href="/" />
        <link rel="stylesheet" type="text/css"
              href="/webjars/bootstrap/css/bootstrap.min.css" />
        <script type="text/javascript" src="/webjars/jquery/jquery.min.js"></script>
        <script type="text/javascript"
                src="/webjars/bootstrap/js/bootstrap.min.js"></script>
    </head>
    <body>
      <h1>SSO sukses</h1>
    </body>
    </html>
  ```
# Bikin UI untuk login
- Bikin UI (resource/static/index.html)
  ```
    ...
    <body ng-app="app" ng-controller="home as home">
      <h1>Login</h1>
      <div class="container" ng-show="!home.authenticated">
          With Facebook: <a href="/login">click here</a>
      </div>
      <div class="container" ng-show="home.authenticated">
          <h1>SSO berhasil</h1>
          Logged in as: <span ng-bind="home.user"></span>
      </div>
      <script type="text/javascript" src="/webjars/angularjs/angular.min.js"></script>
      <script type="text/javascript">
          angular.module("app", []).controller("home", function($http) {
              var self = this;
              $http.get("/user").success(function(data) {
                  self.user = data.userAuthentication.details.name;
                  self.authenticated = true;
              }).error(function() {
                  self.user = "N/A";
                  self.authenticated = false;
              });
          });
      </script>
    </body>
    ...
  ```
- Bikin RestController(java/domain/controllers/IndexController.java)
  ```
  @RestController
  public class IndexController {
      @RequestMapping("/user")
      public Principal user(Principal principal) {
          return principal;
      }
  }
  ```
- Bikin Konfigurasi Security(java/domain/config/KonfigurasiSecurity.java)
  ```
    @Configurable
    @EnableOAuth2Sso
    @EnableWebSecurity
    public class KonfigurasiSecurity extends WebSecurityConfigurerAdapter {
        @Override
        protected void configure(HttpSecurity http) throws Exception {
            http.antMatcher("/**").authorizeRequests().antMatchers("/", "/login**", "/webjars/**").permitAll().anyRequest()
                    .authenticated();
        }
    }
  ```
#Custom Logout
- Konfigurasi client Side (resources/static/index.html)
  ```
    angular
    .module("app", [])
    ...
    .controller("home", function($http, $location) {
      var self = this;
      ...
      self.logout = function() {
        $http.post('/logout', {}).success(function() {
          self.authenticated = false;
          $location.path("/");
        }).error(function(data) {
          console.log("Logout failed")
          self.authenticated = false;
        });
      };
    });
  ```
- Konfigurasi Server Side
  ```
    .and()
    .logout().logoutSuccessUrl("/").permitAll()
    .and()
    .csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
  ```
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
- Masukkan data, sesuaikan dengan dependency yang dibutuhkan(web, security,oauth2) lalu download
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
- Konfigurasi Server Side (java/domain/config/KonfigurasiSecurity.java)
  ```
    .and()
    .logout().logoutSuccessUrl("/").permitAll()
    .and()
    .csrf().csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse());
  ```

#Memisahkan Otorisasi Facebook dari Spring Security
- Mengubah anotasi @EnableOAuth2Sso dengan @EnableOAuth2Client (java/domain/config/KonfigurasiSecurity.java)

     Sejatinya adalah aplikasi yang telah kita buat tadi berdiri di persis di atas Spring Security. Hal ini dikarenakan kita menggunakan anotasi @EnableOAuth2Sso. Anotasi tersebut sendiri terdiri dari 2 fitur yaitu OAuth2 client dan Oauth2 authentification.
     Untuk OAuth2 client dia dapat berinteraksi dengan resource OAuth2 yang disediakan oleh Authorization Server (dalam konteks ini facebook Authorization Server).
     Sedangkan OAuth2 authentification dia berfungsi untuk menyelaraskan aplikasi kita dengan REST milik Spring Security
     Jadi ketika 2 fitur itu digunakan untuk SSO ke facebook saja, maka kita hanya dapat SSO ke facebook saja
     Oleh karena itu kita akan mengganti anotasi @EnableOAuth2Sso dengan @EnableOAuth2Client
  ```
    @Configurable
    @EnableOAuth2Client
    @EnableWebSecurity
    public class KonfigurasiSecurity extends WebSecurityConfigurerAdapter {

    }
  ```
- Membuat Filter authentification
  ```
    @Autowired
    OAuth2ClientContext oauth2ClientContext;

    ///Bean untuk memberitahu filter tentang registrasi client dengan facebook
    @Bean
    @ConfigurationProperties("facebook.client")
    public AuthorizationCodeResourceDetails facebook() {
      return new AuthorizationCodeResourceDetails();
    }

    ///Bean untuk memberitahu filter tentang dimana user end point di facebook
    @Bean
    @ConfigurationProperties("facebook.resource")
    public ResourceServerProperties facebookResource() {
      return new ResourceServerProperties();
    }

    private Filter ssoFilter() {
      OAuth2ClientAuthenticationProcessingFilter facebookFilter = new OAuth2ClientAuthenticationProcessingFilter("/login/facebook");
      OAuth2RestTemplate facebookTemplate = new OAuth2RestTemplate(facebook(), oauth2ClientContext);
      facebookFilter.setRestTemplate(facebookTemplate);
      UserInfoTokenServices tokenServices = new UserInfoTokenServices(facebookResource().getUserInfoUri(), facebook().getClientId());
      tokenServices.setRestTemplate(facebookTemplate);
      facebookFilter.setTokenServices(tokenServices);
      return facebookFilter;
    }


    @Override
    protected void configure(HttpSecurity http) throws Exception {
      http.antMatcher("/**")
      ...
      .addFilterBefore(ssoFilter(), BasicAuthenticationFilter.class);
    }
  ```
- Mengubah konfigurasi OAuth2
  ```
    facebook:
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
    logging:
      level:
        org.springframework.security: DEBUG
  ```
- Ganti URL pada UI
  ```
    <div class="container" ng-show="!home.authenticated">
      <div>
      With Facebook: <a href="/login/facebook">click here</a>
      </div>
    </div>
  ```
- Buat Konfigurasi untuk Redirect
  ```
    @Bean
    public FilterRegistrationBean oauth2ClientFilterRegistration(OAuth2ClientContextFilter filter) {
      FilterRegistrationBean registration = new FilterRegistrationBean();
      registration.setFilter(filter);
      registration.setOrder(-100);
      return registration;
    }

  ```

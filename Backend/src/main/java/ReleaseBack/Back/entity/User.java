package ReleaseBack.Back.entity;

import lombok.Data;

@Data
public class User {

    //mybatis会直接无视@Column等注解，那是JPA的，需要在xml里面自己配置映射
    private Integer id;
    private String username;
    private String password;
    private String email;

    // getter & setter
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
}

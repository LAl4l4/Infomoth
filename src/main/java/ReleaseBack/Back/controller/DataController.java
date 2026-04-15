package ReleaseBack.Back.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import ReleaseBack.Back.service.DataService;

@RestController
@RequestMapping("/data")
@CrossOrigin(origins = "http://localhost:3000") 
public class DataController {
    
    @Autowired
    private DataService dataService;

    @GetMapping("/exchangerate")
    public ResponseEntity<Integer> getExchangeRate(
        @RequestParam String base, 
        @RequestParam String quote) {
        // 直接调用 Service，不需要在 Controller 里写 try-catch
        // 逻辑：如果 Service 抛出异常，Spring 会根据异常类上的 @ResponseStatus 自动返回状态码
        double rate = dataService.getExchangeRate(base, quote);
        
        // 如果你想返回更专业的 JSON 对象（DTO），可以这样写：
        // return ResponseEntity.ok(new ExchangeRateResponseDTO(base, quote, rate));
        
        return ResponseEntity.ok((int) rate);
    }
}

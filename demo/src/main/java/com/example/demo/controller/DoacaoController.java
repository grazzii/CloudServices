package com.example.demo.controller;

import com.example.demo.model.Campanha;
import com.example.demo.model.Doacao;
import com.example.demo.repository.CampanhaRepo;
import com.example.demo.repository.DoacaoRepo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("api/campanhas")
public class DoacaoController {

    @Autowired
    private CampanhaRepo campanhaRepo;

    @Autowired
    private DoacaoRepo doacaoRepo;

    @PutMapping("/{id}/doar")
    public ResponseEntity<String> fazerDoacao(
            @PathVariable Long id,
            @RequestBody Map<String, Object> updates) {
        try {
            // Verificar se a campanha existe
            Optional<Campanha> campanhaOptional = campanhaRepo.findById(id);
            if (!campanhaOptional.isPresent()) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Campanha não encontrada");
            }

            Campanha campanha = campanhaOptional.get();

            // Verificar se o valor da doação foi fornecido
            if (updates.containsKey("valorArrecadado")) {
                BigDecimal valor = new BigDecimal(updates.get("valorArrecadado").toString());

                // Criar a doação associada à campanha
                Doacao doacao = new Doacao();
                doacao.setValor(valor);
                doacao.setDataDoacao(LocalDate.now());
                doacao.setCampanha(campanha);

                // Salvar a doação diretamente na tabela 'doacao'
                doacaoRepo.save(doacao);

                // Atualizar o valor arrecadado da campanha com o valor único da nova doação
                campanha.setValorArrecadado(valor);  // Substitui o valor arrecadado com a nova doação

                // Salvar a campanha atualizada
                campanhaRepo.save(campanha);

                return ResponseEntity.ok("Doação realizada com sucesso");
            }

            return ResponseEntity.badRequest().body("Valor da doação não fornecido");

        } catch (Exception e) {
            // Logar o erro (sem stacktrace)
            System.out.println("Erro ao processar a doação: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Erro ao processar a doação");
        }
    }
}

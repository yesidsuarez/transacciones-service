package com.yesidsuarez.transacciones.controller;

import com.yesidsuarez.transacciones.service.TransaccionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API REST de transacciones. El controlador solo delega: no contiene reglas de negocio *
 * @author YesidSuarez
 */
@RestController
@RequestMapping("/transacciones")
public class TransaccionController {

    private final TransaccionService transaccionService;

    public TransaccionController(TransaccionService transaccionService) {
        this.transaccionService = transaccionService;
    }

    /** Reversa una transacción. Respuesta exitosa: 204 No Content. */
    @PatchMapping("/{id}/reversar")
    public ResponseEntity<Void> reversar(@PathVariable Long id) {
        transaccionService.reversar(id);
        return ResponseEntity.noContent().build();
    }

    /** Consulta una transacción (útil para verificar el cambio de estado). */
    @GetMapping("/{id}")
    public ResponseEntity<TransaccionResponse> consultar(@PathVariable Long id) {
        return ResponseEntity.ok(TransaccionResponse.desde(transaccionService.consultar(id)));
    }

    /** Lista todas las transacciones. */
    @GetMapping
    public ResponseEntity<List<TransaccionResponse>> listar() {
        return ResponseEntity.ok(transaccionService.listar().stream().map(TransaccionResponse::desde).toList());
    }
}

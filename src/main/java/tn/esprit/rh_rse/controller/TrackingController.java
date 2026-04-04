package tn.esprit.rh_rse.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import tn.esprit.rh_rse.dto.PositionDTO;

@Controller
@RequiredArgsConstructor
public class TrackingController {

    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/position/{vehiculeId}")
    public void recevoirPosition(
            @DestinationVariable String vehiculeId,
            PositionDTO position) {

        messagingTemplate.convertAndSend(
                "/topic/navette/" + vehiculeId,
                position
        );
    }
}
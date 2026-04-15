package com.LOLCAA.controller;

import com.LOLCAA.domain.dto.ChampionListItemDTO;
import com.LOLCAA.service.ChampionQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/champions")
@RequiredArgsConstructor
public class ChampionController {

    private final ChampionQueryService championQueryService;

    @GetMapping
    public List<ChampionListItemDTO> listChampions(
            @RequestParam(required = false) String role,
            @RequestParam(required = false) String name
    ) {
        return championQueryService.listChampions(role, name);
    }
}


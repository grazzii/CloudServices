async function asyncLerCampanhas(proxsucesso, proxerro) {
    const URL = "https://sinister-zombie-v4j5rpxq7pq3wr4r-8087.app.github.dev/api/campanhas";
    try {
        const resposta = await fetch(URL);
        if (!resposta.ok) throw new Error("Erro de rede: " + resposta.status);
        const dados = await resposta.json();
        proxsucesso(dados);
    } catch (error) {
        proxerro(error);
    }
}

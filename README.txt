Olá!

Este arquivo contém algumas orientações para configuração e execução das ferramentas que te ajudarão na obtenção de alinhamentos e análise dos mesmos.

# Configuração

methods.properties - Contém os metódos de alinhamento que podem ser utilizados. Descomente os métodos que deseja executar para todas as requisições.
Os alinhamentos serão salvos no diretório "alignments".

requests.properties - Indique neste arquivo quais são os alinhamentos desejados. É importante ressaltar que alinhamento são direcionados, portanto, 1-2 é diferente de 2-1.

ontologies.properties - Arquivo que contém as indicações de quais ontologias serão utilizadas para alinhamentos e análise. A ontologia 0 é a ontologia base, aquela que é usada pelos usuários para adicionar novos conceitos durante o experimento ROO.

analyzers.properties - contém configurações para a análise das ontologias e alinhamentos. Indique neste arquivo, o PREFIX da ontologia base (vocabulário básico) e métodos de análise.

# Execução
Utilize:
./run.sh CLASSE

sendo CLASSE um dos seguintes valores:
AServClient - Para realizar os alinhamentos (para todos os métodos configurados são realizados os alinhamentos indicados em requests.properties)

Analyzer - Para realizar a análise das ontologias e alinhamentos.

PajekGraphGenerator - Gerar grafos que podem ser visualizados na ferramenta Pajek (http://vlado.fmf.uni-lj.si/pub/networks/pajek/).

# Dúvidas
Carlos Fran
carlosphran@gmail.com


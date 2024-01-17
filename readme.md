# fibonacci preview

## Основная информация

`libs/` содержит библиотеку для работы с Jenkins REST API  
`fibonacci/` содержит основной скрипт и файл тестов для него  
> Если необходимо, чтобы юнит-тест провалился, можно раскомментировать последний тест
___
## Пайплайн
```groovy
def environmentAgents = [
    [ name: "Python 2.7", image: "python:2.7-slim" ], 
    [ name: "Python 3.2", image: "python:3.2-slim-internal", dockerfile: 'py3_2.dockerfile' ], 
    [ name: "Python 3.6", image: "python:3.6-slim" ], 
    [ name: "Python 3.8", image: "python:3.8-slim" ], 
    [ name: "Latest Python", image: "python:latest" ] 
]
```
Собственно, перечисление образов docker-image, на базе которых будут создаваться ноды для тестов  
> `Внимание!` python 3.2 слишком **deprecated**. flake8 смог запустить, pylint, сколько бы пакетов не перебирал, разве что ломал и flake8 (sry)  

Для добавление, изменнеия, удаления узлов для тестов, просто меняем этот массив

___
```groovy
def lintTest = [
    [ name: 'flake8', args: ['--format=pylint', '.'] ],
    [ name: 'pylint', args: ['--output-format=text', '*.py'] ]
]
```
Перечисление тулзов и аргументов к ним (динамически) для запуска в контейнере (линтеры)
___
Умышленно не делал падение сборки, чтобы воспроизвести все фазы
В коде есть секции `/**/`, при раскомментировании которых, при некорректной работе кода, будет падение всей сборки.
___
## Graph
![graph](/images/graph.jpg)

___
## History

![hist](/images/hist.jpg)  
Тупо полдня перебора пакетов deprecated версии яп